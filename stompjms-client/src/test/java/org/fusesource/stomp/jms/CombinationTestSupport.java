/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.stomp.jms;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Poor mans way of getting JUnit to run a test case through a few different
 * combinations of options. Usage: If you have a test case called testFoo what
 * you want to run through a few combinations, of of values for the attributes
 * age and color, you would something like: <code>
 * public void initCombosForTestFoo() {
 * addCombinationValues( "age", new Object[]{ new Integer(21), new Integer(30) } );
 * addCombinationValues( "color", new Object[]{"blue", "green"} );
 * }
 * </code>
 * The testFoo test case would be run for each possible combination of age and
 * color that you setup in the initCombosForTestFoo method. Before each
 * combination is run, the age and color fields of the test class are set to one
 * of the values defined. This is done before the normal setUp method is called.
 * If you want the test combinations to show up as separate test runs in the
 * JUnit reports, add a suite method to your test case similar to: <code>
 * public static Test suite() {
 * return suite(FooTest.class);
 * }
 * </code>
 *
 * @version $Revision: 1.5 $
 */
public abstract class CombinationTestSupport extends AutoFailTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CombinationTestSupport.class);

    private HashMap<String, ComboOption> comboOptions = new HashMap<String, ComboOption>();
    private boolean combosEvaluated;
    private Map<String, Object> options;

    static class ComboOption {
        final String attribute;
        final LinkedHashSet<Object> values = new LinkedHashSet<Object>();

        public ComboOption(String attribute, Collection<Object> options) {
            this.attribute = attribute;
            this.values.addAll(options);
        }
    }

    public void addCombinationValues(String attribute, Object[] options) {
        ComboOption co = this.comboOptions.get(attribute);
        if (co == null) {
            this.comboOptions.put(attribute, new ComboOption(attribute, Arrays.asList(options)));
        } else {
            co.values.addAll(Arrays.asList(options));
        }
    }

    public void runBare() throws Throwable {
        if (combosEvaluated) {
            super.runBare();
        } else {
            CombinationTestSupport[] combinations = getCombinations();
            for (int i = 0; i < combinations.length; i++) {
                CombinationTestSupport test = combinations[i];
                if (getName() == null || getName().equals(test.getName())) {
                    test.runBare();
                }
            }
        }
    }

    private void setOptions(Map<String, Object> options) throws NoSuchFieldException, IllegalAccessException {
        this.options = options;
        for (String attribute : options.keySet()) {
            Object value = options.get(attribute);
            try {
                Field field = getClass().getField(attribute);
                field.set(this, value);
            } catch (Throwable e) {
                LOG.info("Could not set field '" + attribute + "' to value '" + value + "', make sure the field exists and is public.");
            }
        }
    }

    private CombinationTestSupport[] getCombinations() {
        try {
            Method method = getClass().getMethod("initCombos", (Class[]) null);
            method.invoke(this, (Object[]) null);
        } catch (Throwable e) {
        }

        String name = getName().split(" ")[0];
        String comboSetupMethodName = "initCombosFor" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            Method method = getClass().getMethod(comboSetupMethodName, (Class[]) null);
            method.invoke(this, (Object[]) null);
        } catch (Throwable e) {
        }

        try {
            ArrayList<HashMap<String, Object>> expandedOptions = new ArrayList<HashMap<String, Object>>();
            expandCombinations(new ArrayList<ComboOption>(comboOptions.values()), expandedOptions);

            if (expandedOptions.isEmpty()) {
                combosEvaluated = true;
                return new CombinationTestSupport[]{this};
            } else {

                ArrayList<CombinationTestSupport> result = new ArrayList<CombinationTestSupport>();
                // Run the test case for each possible combination
                for (Iterator<HashMap<String, Object>> iter = expandedOptions.iterator(); iter.hasNext();) {
                    CombinationTestSupport combo = (CombinationTestSupport) TestSuite.createTest(getClass(), name);
                    combo.combosEvaluated = true;
                    combo.setOptions(iter.next());
                    result.add(combo);
                }

                CombinationTestSupport rc[] = new CombinationTestSupport[result.size()];
                result.toArray(rc);
                return rc;
            }
        } catch (Throwable e) {
            combosEvaluated = true;
            return new CombinationTestSupport[]{this};
        }
    }

    private void expandCombinations(List<ComboOption> optionsLeft, List<HashMap<String, Object>> expandedCombos) {
        if (!optionsLeft.isEmpty()) {
            HashMap<String, Object> map;
            if (comboOptions.size() == optionsLeft.size()) {
                map = new HashMap<String, Object>();
                expandedCombos.add(map);
            } else {
                map = expandedCombos.get(expandedCombos.size() - 1);
            }

            LinkedList<ComboOption> l = new LinkedList<ComboOption>(optionsLeft);
            ComboOption comboOption = l.removeLast();
            int i = 0;
            for (Iterator<Object> iter = comboOption.values.iterator(); iter.hasNext();) {
                Object value = iter.next();
                if (i != 0) {
                    map = new HashMap<String, Object>(map);
                    expandedCombos.add(map);
                }
                map.put(comboOption.attribute, value);
                expandCombinations(l, expandedCombos);
                i++;
            }
        }
    }

    public static Test suite(Class<? extends CombinationTestSupport> clazz) {
        TestSuite suite = new TestSuite();

        ArrayList<String> names = new ArrayList<String>();
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            String name = methods[i].getName();
            if (names.contains(name) || !isPublicTestMethod(methods[i])) {
                continue;
            }
            names.add(name);
            Test test = TestSuite.createTest(clazz, name);
            if (test instanceof CombinationTestSupport) {
                CombinationTestSupport[] combinations = ((CombinationTestSupport) test).getCombinations();
                for (int j = 0; j < combinations.length; j++) {
                    suite.addTest(combinations[j]);
                }
            } else {
                suite.addTest(test);
            }
        }
        return suite;
    }

    private static boolean isPublicTestMethod(Method m) {
        return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
    }

    private static boolean isTestMethod(Method m) {
        String name = m.getName();
        Class<?>[] parameters = m.getParameterTypes();
        Class<?> returnType = m.getReturnType();
        return parameters.length == 0 && name.startsWith("test") && returnType.equals(Void.TYPE);
    }

    public String getName() {
        return getName(false);
    }

    public String getName(boolean original) {
        if (options != null && !original) {
            return super.getName() + " " + options;
        }
        return super.getName();
    }
}
