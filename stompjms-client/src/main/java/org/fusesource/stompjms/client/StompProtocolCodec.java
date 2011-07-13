/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.stompjms.client;

import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.stompjms.client.transport.ProtocolCodec;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import static org.fusesource.stompjms.client.Constants.COLON_BYTE;
import static org.fusesource.stompjms.client.Constants.CONTENT_LENGTH;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class StompProtocolCodec implements ProtocolCodec {
    private static final int max_command_length = 20;
    int max_header_length = 1024*10;
    int max_headers = 1000;
    int max_data_length = 1024 * 1024 * 100;

  /////////////////////////////////////////////////////////////////////
  //
  // Non blocking write imp
  //
  /////////////////////////////////////////////////////////////////////

    int write_buffer_size = 1024*64;
    long write_counter = 0L;
    WritableByteChannel write_channel = null;

    DataByteArrayOutputStream next_write_buffer = new DataByteArrayOutputStream(write_buffer_size);

    ByteBuffer write_buffer = ByteBuffer.allocate(0);
    int write_direct_pos = 0;
    int last_write_io_size = 0;

    public void setWritableByteChannel(WritableByteChannel channel) throws SocketException {
        this.write_channel = channel;
        if( this.write_channel instanceof SocketChannel) {
          write_buffer_size = ((SocketChannel)this.write_channel).socket().getSendBufferSize();
        }
    }

    public boolean full() {
        return next_write_buffer.size() >= (write_buffer_size >> 1);
    }

    public boolean is_empty() {
        return write_buffer.remaining() == 0;
    }
    public long getWriteCounter() {
        return write_counter;
    }

    public int getLastWriteSize() {
        return last_write_io_size;
    }

    public BufferState write(Object value) throws IOException {
        if ( full() ) {
          return ProtocolCodec.BufferState.FULL;
        } else {
          boolean was_empty = is_empty();
          ((StompFrame)value).write(next_write_buffer);
          if( was_empty ) {
            return ProtocolCodec.BufferState.WAS_EMPTY;
          } else {
            return ProtocolCodec.BufferState.NOT_EMPTY;
          }
        }
    }

    public BufferState flush() throws IOException {
        // if we have a pending write that is being sent over the socket...
        if ( write_buffer.remaining() != 0 ) {
          last_write_io_size = write_channel.write(write_buffer);
          write_counter += last_write_io_size;
        }

        // if it is now empty try to refill...
        if ( is_empty() ) {
            // size of next buffer is based on how much was used in the previous buffer.
            int prev_size = Math.min(Math.max((write_buffer.position() + 512), 512), write_buffer_size);
            write_buffer = next_write_buffer.toBuffer().toByteBuffer();
            next_write_buffer = new DataByteArrayOutputStream(prev_size);
        }

        if ( is_empty() ) {
          return ProtocolCodec.BufferState.EMPTY;
        } else {
          return ProtocolCodec.BufferState.NOT_EMPTY;
        }
    }

  /////////////////////////////////////////////////////////////////////
  //
  // Non blocking read impl
  //
  /////////////////////////////////////////////////////////////////////

  interface FrameReader {
      StompFrame apply(ByteBuffer buffer) throws IOException;
  }

  long read_counter = 0L;
  int read_buffer_size = 1024*64;
  ReadableByteChannel read_channel = null;

  ByteBuffer read_buffer = ByteBuffer.allocate(read_buffer_size);
  int read_end = 0;
  int read_start = 0;
  int last_read_io_size = 0;

  FrameReader next_action = read_action();
  boolean trim = true;

    public void setReadableByteChannel(ReadableByteChannel channel) throws SocketException {
        this.read_channel = channel;
        if( this.read_channel instanceof SocketChannel ) {
          read_buffer_size = ((SocketChannel)this.read_channel).socket().getReceiveBufferSize();
        }
    }

    public void unread(Buffer buffer) {
        assert(read_counter == 0);
        read_buffer.put(buffer.data, buffer.offset, buffer.length);
        read_counter += buffer.length;
    }

    public long getReadCounter() {
        return read_counter;
    }

    public int getLastReadSize() {
        return last_read_io_size;
    }

    public Object read() throws IOException {
        Object command = null;
        while( command==null ) {
          // do we need to read in more data???
          if (read_end == read_buffer.position() ) {

              // do we need a new data buffer to read data into??
              if (read_buffer.remaining() == 0) {

                  // How much data is still not consumed by the wireformat
                  int size = read_end - read_start;

                  int new_capacity;
                  if(read_start == 0) {
                    new_capacity = size+read_buffer_size;
                  } else {
                    if (size > read_buffer_size) {
                      new_capacity = size+read_buffer_size;
                    } else {
                      new_capacity = read_buffer_size;
                    }
                  }

                  byte[] new_buffer = new byte[new_capacity];
                  if (size > 0) {
                      System.arraycopy(read_buffer.array(), read_start, new_buffer, 0, size);
                  }

                  read_buffer = ByteBuffer.wrap(new_buffer);
                  read_buffer.position(size);
                  read_start = 0;
                  read_end = size;
              }

              // Try to fill the buffer with data from the socket..
              int p = read_buffer.position();
              last_read_io_size = read_channel.read(read_buffer);
              if (last_read_io_size == -1) {
                  throw new EOFException("Peer disconnected");
              } else if (last_read_io_size == 0) {
                  return null;
              }
              read_counter += last_read_io_size;
          }

          command = next_action.apply(read_buffer);

          // Sanity checks to make sure the codec is behaving as expected.
          assert(read_start <= read_end);
          assert(read_end <= read_buffer.position());
        }
        return command;
    }


  Buffer read_line(ByteBuffer buffer, int max, String errorMessage) throws IOException {
      int read_limit = buffer.position();
      while( read_end < read_limit ) {
        if( buffer.array()[read_end] =='\n') {
          Buffer rc = new Buffer(buffer.array(), read_start, read_end-read_start);
          read_end += 1;
          read_start = read_end;
          return rc;
        }
        if (max != -1 && read_end-read_start > max) {
            throw new IOException(errorMessage);
        }
        read_end += 1;
      }
      return null;
  }

  FrameReader read_action() {
    return new FrameReader() {
        public StompFrame apply(ByteBuffer buffer) throws IOException {
            Buffer line = read_line(buffer, max_command_length, "The maximum command length was exceeded");
            if( line !=null ) {
              Buffer action = line;
              if( trim ) {
                  action = action.trim();
              }
              if (action.length() > 0) {
                  StompFrame frame = new StompFrame(action.ascii());
                  next_action = read_headers(frame);
              }
            }
            return null;
        }
    };
  }


    FrameReader read_headers(final StompFrame frame) {
      final AsciiBuffer[] contentLengthValue = new AsciiBuffer[1];
      final ArrayList<StompFrame.HeaderEntry> headers = new ArrayList<StompFrame.HeaderEntry>(10);
      return new FrameReader() {
          public StompFrame apply(ByteBuffer buffer) throws IOException {
              Buffer line = read_line(buffer, max_header_length, "The maximum header length was exceeded");
              if( line !=null ) {
                if( line.trim().length > 0 ) {

                  if (max_headers != -1 && headers.size() > max_headers) {
                      throw new IOException("The maximum number of headers was exceeded");
                  }

                  try {
                      int seperatorIndex = line.indexOf(COLON_BYTE);
                      if( seperatorIndex<0 ) {
                          throw new IOException("Header line missing seperator [" + line.ascii() + "]");
                      }
                      Buffer name = line.slice(0, seperatorIndex);
                      if( trim ) {
                          name = name.trim();
                      }

                      Buffer value = line.slice(seperatorIndex + 1, line.length());
                      if( trim ) {
                          value = value.trim();
                      }
                      StompFrame.HeaderEntry entry = new StompFrame.HeaderEntry(name.ascii(), value.ascii());
                      if( entry.key.equals(CONTENT_LENGTH) ) {
                          contentLengthValue[0] = entry.value;
                      }
                      headers.add(entry);
                  } catch(Exception e) {
                    throw new IOException("Unable to parser header line [" + line + "]");
                  }

                } else {
                  frame.setHeaders(headers);
                  AsciiBuffer contentLength = contentLengthValue[0];
                  if (contentLength!=null) {
                    // Bless the client, he's telling us how much data to read in.
                    int length=0;
                    try {
                        length = Integer.parseInt(contentLength.toString());
                    } catch(NumberFormatException e) {
                        throw new IOException("Specified content-length is not a valid integer");
                    }

                    if (max_data_length != -1 && length > max_data_length) {
                        throw new IOException("The maximum data length was exceeded");
                    }

                    next_action = read_binary_body(frame, length);
                  } else {
                    next_action = read_text_body(frame);
                  }
                }
              }
              return null;
          }
      };
    }

    FrameReader read_binary_body(final StompFrame frame, final int contentLength) {
        return new FrameReader() {
            public StompFrame apply(ByteBuffer buffer) throws IOException {
                Buffer content=read_content(buffer, contentLength);
                if( content != null ) {
                  frame.content(content);
                  next_action = read_action();
                  return frame;
                } else {
                  return null;
                }
            }
        };
      }



  Buffer read_content(ByteBuffer buffer, int contentLength) throws IOException {
      int read_limit = buffer.position();
      if( (read_limit-read_start) < contentLength+1 ) {
        read_end = read_limit;
        return null;
      } else {
        if( buffer.array()[read_start+contentLength]!= 0 ) {
           throw new IOException("Expected null termintor after "+contentLength+" content bytes");
        }
        Buffer rc = new Buffer(buffer.array(), read_start, contentLength);
        read_end = read_start+contentLength+1;
        read_start = read_end;
        return rc;
      }
  }

    FrameReader read_text_body(final StompFrame frame) {
      return new FrameReader() {
          public StompFrame apply(ByteBuffer buffer) throws IOException {
              Buffer content=read_to_null(buffer);
              if( content != null ) {
                next_action = read_action();
                frame.content(content);
                return frame;
              } else {
                return null;
              }
          }
      };
    }

    Buffer read_to_null(ByteBuffer buffer) {
        int read_limit = buffer.position();
        byte[] array = buffer.array();
        while( read_end < read_limit ) {
            if( array[read_end] ==0) {
            Buffer rc = new Buffer(array, read_start, read_end-read_start);
            read_end += 1;
            read_start = read_end;
            return rc;
          }
          read_end += 1;
        }
        return null;
    }

}
