/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy;

import groovy.lang.Closure;
import groovy.lang.Reference;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.*;

/**
 * Groovy's Closure class isn't specifically designed with Java integration in
 * mind, but these tests illustrate some of the possible ways to use them from Java.
 */
public class ClosureJavaIntegrationTest extends TestCase {
    Map<String,  Integer> zoo = new HashMap<String,  Integer>();
    List<String> animals = Arrays.asList("ant", "bear", "camel");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        zoo.put("Monkeys", 3);
        zoo.put("Giraffe", 2);
        zoo.put("Lions", 5);
    }

    public void testJoinListNonClosureCase() {
        assertEquals(join(animals, ", "), "ant, bear, camel");
    }

    public void testEachList() {
        final List<Integer> result = new ArrayList<Integer>();
        each(animals, new Closure() {
            public void doCall(String arg) {
                result.add(arg.length());
            }
        });
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    public void testEachMap() {
        final List<String> result = new ArrayList<String>();
        each(zoo, new Closure() {
            public void doCall(String k, Integer v) {
                result.add("k=" + k + ",v=" + v);
            }
        });
        assertEquals(Arrays.asList("k=Lions,v=5", "k=Monkeys,v=3", "k=Giraffe,v=2"), result);
    }

    public void testCollectList() {
        assertEquals(Arrays.asList(3, 4, 5), collect(animals, new Closure<Integer>() {
            public Integer doCall(String it) {
                return it.length();
            }
        }));
    }

    public void testMaxMap() {
        Map.Entry<String, Integer> lionEntry = null;
        for (Map.Entry<String, Integer> entry : zoo.entrySet()) {
            if (entry.getKey().equals("Lions")) lionEntry = entry;
        }
        assertEquals(lionEntry, max(zoo.entrySet(), new Closure<Integer>() {
            public Integer doCall(Map.Entry<String, Integer> e) {
                return e.getKey().length() * e.getValue();
            }
        }));
    }

    public void testSortMapKeys() {
        assertEquals(Arrays.asList("Monkeys", "Lions", "Giraffe"), sort(zoo.keySet(), new Closure<Integer>() {
            public Integer doCall(String a, String b) {
                return -a.compareTo(b);
            }
        }));
        assertEquals(Arrays.asList("Giraffe", "Lions", "Monkeys"), sort(zoo.keySet(), new Closure<Integer>() {
            public Integer doCall(String a, String b) {
                return a.compareTo(b);
            }
        }));
    }

    public void testAnyMap() {
        assertTrue(any(zoo, new Closure<Boolean>() {
            public Boolean doCall(String k, Integer v) {
                return k.equals("Lions") && v == 5;
            }
        }));
    }

    public void testFindAllAndCurry() {
        Map<String, Integer> expected = new HashMap<String, Integer>(zoo);
        expected.remove("Lions");
        Closure<Boolean> keyBiggerThan = new Closure<Boolean>() {
            public Boolean doCall(Map.Entry<String, Integer> e, Integer size) {
                return e.getKey().length() > size;
            }
        };
        Closure<Boolean> keyBiggerThan6 = keyBiggerThan.rcurry(new Object[]{6});
        assertEquals(expected, findAll(zoo, keyBiggerThan6));
    }

    public void testListArithmetic() {
        List<List> numLists = new ArrayList<List>();
        numLists.add(Arrays.asList(1, 2, 3));
        numLists.add(Arrays.asList(10, 20, 30));
        assertEquals(Arrays.asList(6, 60), collect(numLists, new Closure<Integer>() {
            public Integer doCall(Integer a, Integer b, Integer c) {
                return a + b + c;
            }
        }));
        Closure<Integer> arithmeticClosure = new Closure<Integer>() {
            public Integer doCall(Integer a, Integer b, Integer c) {
                return a * b + c;
            }
        };
        Closure<Integer> tensAndUnits = arithmeticClosure.curry(new Object[]{10});
        assertEquals(35, (int)tensAndUnits.call(new Object[]{3, 5}));
        tensAndUnits = arithmeticClosure.ncurry(0, new Object[]{10});
        assertEquals(35, (int)tensAndUnits.call(new Object[]{3, 5}));
        tensAndUnits = arithmeticClosure.ncurry(1, new Object[]{10});
        assertEquals(35, (int)tensAndUnits.call(new Object[]{3, 5}));
        Closure<Integer> timesPlus5 = arithmeticClosure.rcurry(new Object[]{5});
        assertEquals(35, (int)timesPlus5.call(new Object[]{15, 2}));
        timesPlus5 = arithmeticClosure.ncurry(2, new Object[]{5});
        assertEquals(35, (int)timesPlus5.call(new Object[]{15, 2}));
    }

    public void testComposition() {
        Closure<String> toUpperCase = new Closure<String>() {
            public String doCall(String s) {
                return s.toUpperCase();
            }
        };
        Closure<Boolean> hasCapitalA = new Closure<Boolean>() {
            public Boolean doCall(String s) {
                return s.contains("A");
            }
        };
        Closure<Boolean> hasA = toUpperCase.rightShift(hasCapitalA);
        assertTrue(every(animals, hasA));
        Closure<Boolean> alsoHasA = hasCapitalA.leftShift(toUpperCase);
        assertTrue(every(animals, alsoHasA));
    }

    public void testTrampoline() {
        final Reference<Closure<BigInteger>> ref = new Reference<Closure<BigInteger>>();
        ref.set(new Closure<BigInteger>() {
            public Object doCall(Integer n, BigInteger total) {
                return n > 1 ? ref.get().trampoline(n - 1, total.multiply(BigInteger.valueOf(n))) : total;
            }
        }.trampoline());
        Closure<BigInteger> factorial = new Closure<BigInteger>() {
            public BigInteger doCall(Integer n) {
                return ref.get().call(n, BigInteger.ONE);
            }
        };
        assertEquals(BigInteger.valueOf(479001600), factorial.call(12));
    }
}