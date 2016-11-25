package com.aol.cyclops.scala.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.POrderedSet;

import com.aol.cyclops.Reducer;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.reactor.collections.extensions.persistent.LazyPOrderedSetX;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;
import reactor.core.publisher.Flux;
import scala.collection.JavaConversions;
import scala.collection.immutable.BitSet;
import scala.collection.immutable.BitSet$;
import scala.collection.mutable.Builder;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScalaBitsetPOrderedSet extends AbstractSet<Integer>implements POrderedSet<Integer> {

    /**
     * Create a LazyPOrderedSetX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPOrderedSetX
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<Integer> fromStream(Stream<Integer> stream) {
        return new LazyPOrderedSetX(
                                  Flux.from(ReactiveSeq.fromStream(stream)), 
                                  ScalaBitsetPOrderedSet.toPOrderedSet());
    }

    /**
     * Create a LazyPOrderedSetX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range SetX
     */
    public static LazyPOrderedSetX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

   

    /**
     * Unfold a function into a SetX
     * 
     * <pre>
     * {@code 
     *  LazyPOrderedSetX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return SetX generated by unfolder function
     */
    public static  LazyPOrderedSetX<Integer> unfold(Integer seed, Function<? super Integer, Optional<Tuple2<Integer, Integer>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPOrderedSetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate SetX elements
     * @return SetX generated from the provided Supplier
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX generate(long limit, Supplier s) {

        return fromStream(ReactiveSeq.generate(s)
                                     .limit(limit));
    }

    /**
     * Create a LazyPOrderedSetX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return SetX generated by iterative application
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX iterate(long limit, final T seed, final UnaryOperator f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                     .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * POrderedSet<Integer> q = JSPOrderedSet.<Integer>toPOrderedSet()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for POrderedSet
     */
    public static  Reducer<POrderedSet<Integer>> toPOrderedSet() {
        return Reducer.<POrderedSet<Integer>> of(ScalaBitsetPOrderedSet.emptyPOrderedSet(), 
                                                 (final POrderedSet<Integer> a) -> b -> a.plusAll(b),
                                      (final Integer x) -> ScalaBitsetPOrderedSet.singleton(x));
    }
    
  

    public static  ScalaBitsetPOrderedSet fromSet(BitSet set) {
        return new ScalaBitsetPOrderedSet(
                                 set);
    }

    
    public static  ScalaBitsetPOrderedSet emptyPOrderedSet() {
        return new ScalaBitsetPOrderedSet(
                                 BitSet$.MODULE$.empty());
    }
    
    
    public static LazyPOrderedSetX<Integer> empty() {
        
        
        return LazyPOrderedSetX.fromPOrderedSet(new ScalaBitsetPOrderedSet(BitSet$.MODULE$.empty()),
                                                toPOrderedSet());
    }

    public static  LazyPOrderedSetX<Integer> singleton(Integer t) {
        return of(t);
    }
   
    public static  LazyPOrderedSetX<Integer> of(Integer... t) {

        Builder<Integer, BitSet> lb = (Builder)BitSet$.MODULE$.newBuilder();
       for (Integer next : t)
           lb.$plus$eq(next);
       BitSet vec = lb.result();
       return LazyPOrderedSetX.fromPOrderedSet(new ScalaBitsetPOrderedSet(
                                                       vec),
                                     toPOrderedSet());
   }

  
    

    public static  LazyPOrderedSetX<Integer> POrderedSet(BitSet q) {
        return LazyPOrderedSetX.fromPOrderedSet(new ScalaBitsetPOrderedSet(
                                                         q),
                                      toPOrderedSet());
    }

    @SafeVarargs
    public static  LazyPOrderedSetX<Integer> POrderedSet(Integer... elements) {
        return LazyPOrderedSetX.fromPOrderedSet(of(elements), toPOrderedSet());
    }

    @Wither
    private final BitSet set;

    @Override
    public ScalaBitsetPOrderedSet plus(Integer e) {
       
        return withSet(set.$plus((int)e));
    }

    @Override
    public ScalaBitsetPOrderedSet plusAll(Collection<? extends Integer> l) {
        
        
        BitSet vec = set;
        for (Integer next : l) {
              vec = vec.$plus((int)next);
        }

        return withSet(vec);
       
    }

   

    
  

    @Override
    public POrderedSet<Integer> minus(Object e) {
        if(e instanceof Integer){
            Integer i =(Integer)e;
            return withSet((BitSet)set.$minus$minus(BitSet$.MODULE$.empty().$plus((int)i)));
        }
        else
            return this;
                
    }

    @Override
    public POrderedSet<Integer> minusAll(Collection<?> s) {
        
        return withSet((BitSet)set.$minus$minus(JavaConversions.collectionAsScalaIterable(s)));        
    }

  
   

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Iterator<Integer> iterator() {
        return (Iterator)JavaConversions.asJavaIterator(set.iterator());
    }

    @Override
    public Integer get(int index) {
        return (Integer) set.toIndexedSeq().toVector().apply(index);
    }

    @Override
    public int indexOf(Object o) {
        return set.toIndexedSeq().toVector().indexOf(o);
    }

   

}
