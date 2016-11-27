package com.aol.cyclops.scala.collections;

import static com.aol.cyclops.scala.collections.Converters.ordering;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
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
import scala.collection.GenTraversableOnce;
import scala.collection.JavaConversions;
import scala.collection.generic.CanBuildFrom;
import scala.collection.immutable.TreeSet;
import scala.collection.immutable.TreeSet$;
import scala.collection.mutable.Builder;
import scala.math.Ordering;
import scala.math.Ordering$;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScalaTreePOrderedSet<T> extends AbstractSet<T>implements POrderedSet<T>, HasScalaCollection<T> {

    /**
     * Create a LazyPOrderedSetX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyPOrderedSetX
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> fromStream(Stream<T> stream) {
        return new LazyPOrderedSetX<T>(
                                  Flux.from(ReactiveSeq.fromStream(stream)), 
                                  ScalaTreePOrderedSet.<T>toPOrderedSet(Comparator.naturalOrder()));
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
     * Create a LazyPOrderedSetX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range SetX
     */
    public static LazyPOrderedSetX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
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
    public static <U, T extends Comparable<? super T>> LazyPOrderedSetX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyPOrderedSetX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate SetX elements
     * @return SetX generated from the provided Supplier
     */
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> generate(long limit, Supplier<T> s) {

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
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
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
    public static <T extends Comparable<? super T>>  Reducer<POrderedSet<T>> toPOrderedSet() {
        return Reducer.<POrderedSet<T>> of(ScalaTreePOrderedSet.emptyPOrderedSet(), (final POrderedSet<T> a) -> b -> a.plusAll(b),
                                      (final T x) -> ScalaTreePOrderedSet.singleton(x));
    }
    
    public static <T>  Reducer<POrderedSet<T>> toPOrderedSet(Comparator<T> ordering) {
        return Reducer.<POrderedSet<T>> of(ScalaTreePOrderedSet.emptyPOrderedSet(ordering), 
                                           (final POrderedSet<T> a) -> b -> a.plusAll(b),
                                      (final T x) -> ScalaTreePOrderedSet.singleton(ordering,x));
    }

    public static <T> ScalaTreePOrderedSet<T> fromSet(TreeSet<T> set) {
        return new ScalaTreePOrderedSet<>(
                                 set);
    }

    
    public static <T extends Comparable<? super T>> ScalaTreePOrderedSet<T> emptyPOrderedSet() {
        return new ScalaTreePOrderedSet<>(
                                 TreeSet$.MODULE$.empty(Converters.<T>ordering(Comparator.naturalOrder())));
    }
    
    public static <T> ScalaTreePOrderedSet<T> emptyPOrderedSet(Comparator<T> ordering) {
        return new ScalaTreePOrderedSet<>(
                                 TreeSet$.MODULE$.empty(ordering(ordering)));
    }

    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> empty() {
        
        return LazyPOrderedSetX.fromPOrderedSet(new ScalaTreePOrderedSet<>(
                TreeSet$.MODULE$.empty(Converters.<T>ordering(Comparator.naturalOrder()))),
                                                toPOrderedSet());
    }
    public static <T> LazyPOrderedSetX<T> empty(Comparator<T> comp) {
        
        return LazyPOrderedSetX.fromPOrderedSet(new ScalaTreePOrderedSet<>(
                TreeSet$.MODULE$.empty(Converters.<T>ordering(comp))),
                                                toPOrderedSet(comp));
    }

    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> singleton(T t) {
        return of(t);
    }
    public static <T> LazyPOrderedSetX<T> singleton(Comparator<T> comp,T t) {
        return of(comp,t);
    }
    public static <T> LazyPOrderedSetX<T> of(Comparator<T> comp,T... t) {

        Builder<T, TreeSet<T>> lb = TreeSet$.MODULE$.newBuilder(Converters.<T>ordering(comp));
       for (T next : t)
           lb.$plus$eq(next);
       TreeSet<T> vec = lb.result();
       return LazyPOrderedSetX.fromPOrderedSet(new ScalaTreePOrderedSet<>(
                                                       vec),
                                     toPOrderedSet(comp));
   }

    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> of(T... t) {

        return of(Comparator.naturalOrder(),t);
    }
    

    public static <T> LazyPOrderedSetX<T> POrderedSet(TreeSet<T> q) {
        return LazyPOrderedSetX.fromPOrderedSet(new ScalaTreePOrderedSet<T>(
                                                         q),
                                      toPOrderedSet(q.ordering()));
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> LazyPOrderedSetX<T> POrderedSet(T... elements) {
        return LazyPOrderedSetX.fromPOrderedSet(of(elements), toPOrderedSet());
    }

    @Wither
    private final TreeSet<T> set;

    @Override
    public ScalaTreePOrderedSet<T> plus(T e) {
       
        return withSet(set.$plus(e));
    }

    @Override
    public ScalaTreePOrderedSet<T> plusAll(Collection<? extends T> l) {
        
        TreeSet<T> use =HasScalaCollection.visit(l, scala->set, java->{
            TreeSet<T> vec = set;
            for (T next : l) {
                  vec = vec.$plus(next);
            }
            return vec;
        });
        

        return withSet(use);
       
    }

   

    
  

    @Override
    public POrderedSet<T> minus(Object e) {
        return withSet(set.$minus((T)e));
        
    }

    @Override
    public POrderedSet<T> minusAll(Collection<?> s) {
        
        GenTraversableOnce<T> col = HasScalaCollection.<T>traversable((Collection)s);
        return withSet((TreeSet)set.$minus$minus(col));        
    }

  
   

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Iterator<T> iterator() {
        return JavaConversions.asJavaIterator(set.iterator());
    }

    @Override
    public T get(int index) {
        return set.toIndexedSeq().toVector().apply(index);
    }

    @Override
    public int indexOf(Object o) {
        return set.toIndexedSeq().toVector().indexOf(o);
    }

    @Override
    public GenTraversableOnce<T> traversable() {
        return set;
    }

    @Override
    public CanBuildFrom canBuildFrom() {
        return TreeSet.newCanBuildFrom(set.ordering());
    }

   

}
