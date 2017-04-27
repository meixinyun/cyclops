package com.aol.cyclops.vavr.hkt.typeclesses.instances;
import static com.aol.cyclops.vavr.hkt.ListKind.widen;
import static com.aol.cyclops.util.function.Lambda.l1;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.function.Function;

import com.aol.cyclops.vavr.hkt.ListKind;
import org.junit.Test;

import com.aol.cyclops.control.Maybe;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.hkt.alias.Higher;
import com.aol.cyclops.hkt.cyclops.MaybeType;
import com.aol.cyclops.hkt.instances.cyclops.MaybeInstances;
import com.aol.cyclops.vavr.hkt.typeclasses.instances.ListInstances;
import com.aol.cyclops.util.function.Lambda;

import javaslang.collection.List;

public class ListTest {

    @Test
    public void unit(){
        
        ListKind<String> list = ListInstances.unit()
                                     .unit("hello")
                                     .convert(ListKind::narrowK);
        
        assertThat(list.toJavaList(),equalTo(Arrays.asList("hello")));
    }
    @Test
    public void functor(){
        
        ListKind<Integer> list = ListInstances.unit()
                                     .unit("hello")
                                     .then(h->ListInstances.functor().map((String v) ->v.length(), h))
                                     .convert(ListKind::narrowK);
        
        assertThat(list.toJavaList(),equalTo(Arrays.asList("hello".length())));
    }
    @Test
    public void apSimple(){
        ListInstances.zippingApplicative()
            .ap(widen(List.of(l1(this::multiplyByTwo))),widen(List.of(1,2,3)));
    }
    private int multiplyByTwo(int x){
        return x*2;
    }
    @Test
    public void applicative(){
        
        ListKind<Function<Integer,Integer>> listFn =ListInstances.unit().unit(Lambda.l1((Integer i) ->i*2)).convert(ListKind::narrowK);
        
        ListKind<Integer> list = ListInstances.unit()
                                     .unit("hello")
                                     .then(h->ListInstances.functor().map((String v) ->v.length(), h))
                                     .then(h->ListInstances.zippingApplicative().ap(listFn, h))
                                     .convert(ListKind::narrowK);
        
        assertThat(list.toJavaList(),equalTo(Arrays.asList("hello".length()*2)));
    }
    @Test
    public void monadSimple(){
       ListKind<Integer> list  = ListInstances.monad()
                                      .flatMap(i->widen(ReactiveSeq.range(0,i)), widen(List.of(1,2,3)))
                                      .convert(ListKind::narrowK);
    }
    @Test
    public void monad(){
        
        ListKind<Integer> list = ListInstances.unit()
                                     .unit("hello")
                                     .then(h->ListInstances.monad().flatMap((String v) ->ListInstances.unit().unit(v.length()), h))
                                     .convert(ListKind::narrowK);
        
        assertThat(list.toJavaList(),equalTo(Arrays.asList("hello".length())));
    }
    @Test
    public void monadZeroFilter(){
        
        ListKind<String> list = ListInstances.unit()
                                     .unit("hello")
                                     .then(h->ListInstances.monadZero().filter((String t)->t.startsWith("he"), h))
                                     .convert(ListKind::narrowK);
        
        assertThat(list.toJavaList(),equalTo(Arrays.asList("hello")));
    }
    @Test
    public void monadZeroFilterOut(){
        
        ListKind<String> list = ListInstances.unit()
                                     .unit("hello")
                                     .then(h->ListInstances.monadZero().filter((String t)->!t.startsWith("he"), h))
                                     .convert(ListKind::narrowK);
        
        assertThat(list.toJavaList(),equalTo(Arrays.asList()));
    }
    
    @Test
    public void monadPlus(){
        ListKind<Integer> list = ListInstances.<Integer>monadPlus()
                                      .plus(ListKind.widen(List.empty()), ListKind.widen(List.of(10)))
                                      .convert(ListKind::narrowK);
        assertThat(list.toJavaList(),equalTo(Arrays.asList(10)));
    }
/**
    @Test
    public void monadPlusNonEmpty(){
        
        Monoid<ListKind<Integer>> m = Monoid.of(ListKind.widen(List.empty()), (a,b)->a.isEmpty() ? b : a);
        ListKind<Integer> list = ListInstances.<Integer>monadPlus(m)
                                      .plus(ListKind.widen(List.of(5)), ListKind.widen(List.of(10)))
                                      .convert(ListKind::narrowK);
        assertThat(list,equalTo(Arrays.asList(5)));
    }
**/
    @Test
    public void  foldLeft(){
        int sum  = ListInstances.foldable()
                        .foldLeft(0, (a,b)->a+b, ListKind.widen(List.of(1,2,3,4)));
        
        assertThat(sum,equalTo(10));
    }
    @Test
    public void  foldRight(){
        int sum  = ListInstances.foldable()
                        .foldRight(0, (a,b)->a+b, ListKind.widen(List.of(1,2,3,4)));
        
        assertThat(sum,equalTo(10));
    }
    @Test
    public void traverse(){
       MaybeType<Higher<ListKind.µ, Integer>> res = ListInstances.traverse()
                                                         .traverseA(MaybeInstances.applicative(), (Integer a)->MaybeType.just(a*2), ListKind.just(1,2,3))
                                                         .convert(MaybeType::narrowK);
       
       
       assertThat(res.map(i->i.convert(ListKind::narrowK).toJavaList()),
                  equalTo(Maybe.just(ListX.of(2,4,6))));
    }
    
}