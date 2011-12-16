/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package org.fusesource.mqtt.client.callback;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class Listener {
    public void onPublish(UTF8Buffer topic, Buffer payload, Runnable onComplete) {
        onComplete.run();
    }
    public void failure(Throwable value) {
    }
}
