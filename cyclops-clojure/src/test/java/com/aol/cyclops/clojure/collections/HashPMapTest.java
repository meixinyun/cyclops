package com.aol.cyclops.clojure.collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.aol.cyclops.data.collections.extensions.persistent.PMapX;
import com.aol.cyclops.data.collections.extensions.standard.MapXs;

public class HashPMapTest {
    PMapX<Integer,String> org;
    PMapX<Integer,String> test;
    
    @Before
    public void setup(){
       org = PMapX.fromMap(MapXs.of(1, "hello"));
       test = ClojureHashPMap.singleton(1, "hello");
     
    }
    @Test
    public void same(){
        assertThat(org,equalTo(test));
    }
    @Test
    public void plus(){
        assertThat(org.plus(2, "world"),equalTo(test.plus(2,"world")));
    }
    @Test
    public void minus(){
        assertThat(org.minus(1),equalTo(test.minus(1)));
    }
    @Test
    public void minusAll(){
        assertThat(org.plus(2, "world").minusAll(Arrays.asList(1,2)),
                   equalTo(test.plus(2,"world").minusAll(Arrays.asList(1,2))));
    }
    
    @Test
    public void map(){
        assertThat(test.map(s->s+" world").toListX(t->t.v2).get(0),equalTo("hello world"));
    }
}
