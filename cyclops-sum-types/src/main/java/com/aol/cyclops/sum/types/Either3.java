package com.aol.cyclops.sum.types;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import com.aol.cyclops.Monoid;
import com.aol.cyclops.control.AnyM;
import com.aol.cyclops.control.Eval;
import com.aol.cyclops.control.Ior;
import com.aol.cyclops.control.Matchable.CheckValue1;
import com.aol.cyclops.control.Maybe;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.control.Trampoline;
import com.aol.cyclops.control.Xor;
import com.aol.cyclops.data.collections.extensions.CollectionX;
import com.aol.cyclops.data.collections.extensions.standard.ListX;
import com.aol.cyclops.types.BiFunctor;
import com.aol.cyclops.types.Combiner;
import com.aol.cyclops.types.Filterable;
import com.aol.cyclops.types.Functor;
import com.aol.cyclops.types.MonadicValue;
import com.aol.cyclops.types.To;
import com.aol.cyclops.types.Value;
import com.aol.cyclops.types.anyM.AnyMValue;
import com.aol.cyclops.types.applicative.ApplicativeFunctor;
import com.aol.cyclops.types.stream.reactive.ValueSubscriber;
import com.aol.cyclops.util.function.QuadFunction;
import com.aol.cyclops.util.function.TriFunction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * A right biased Lazy Either3 type. map / flatMap operators are tail-call optimized
 * 
 * Can be one of 3 types
 * 
 * 
 * 
 * @author johnmcclean
 *
 * @param <LT1> Left1 type
 * @param <LT2> Left2 type
 * @param <RT> Right type (operations are performed on this type if present)
 */
public interface Either3<LT1, LT2, RT>
                extends Functor<RT>, BiFunctor<LT2, RT>, Filterable<RT>, MonadicValue<RT>,To<Either3<LT1, LT2, RT>>, Supplier<RT>, ApplicativeFunctor<RT> {
    
    static <LT1,LT2,RT> Either3<LT1,LT2,RT> fromMonadicValue(MonadicValue<RT> mv3){
        if(mv3 instanceof Either3){
            return (Either3)mv3;
        }
        return mv3.toOptional().isPresent()? Either3.right(mv3.get()) : Either3.left1(null);

    }
    /**
     * Create an AnyMValue instance that wraps an Either3
     * 
     * @param xor Xor to wrap inside an AnyM
     * @return AnyM instance that wraps the provided Either3
     */
    public static <LT1,LT2,T> AnyMValue<T> anyM(final Either3<LT1, LT2, T> xor) {
        Objects.requireNonNull(xor);
        return AnyM.ofValue(xor);
    }

    /**
     * Take an iterable containing Either3s and convert them into a List of AnyMs
     * e.g.
     * {@code 
     *     List<AnyM<Integer>> anyMs = anyMList(Arrays.asList(Either3.right(1),Either3.left(10));
     *     
     *     //List[AnyM[Either3:right[1],Either3:left[10]]]
     * }
     * 
     * @param anyM Iterable containing Eithers
     * @return List of AnyMs
     */
    public static <ST, LT2, T> ListX<AnyMValue<T>> anyMList(final Iterable<Either3<ST, LT2, T>> anyM) {
        return ReactiveSeq.fromIterable(anyM)
                          .map(e -> anyM(e))
                          .toListX();
    }

    /**
     *  Turn a collection of Either3 into a single Either with Lists of values.
     *  
     * <pre>
     * {@code 
     * 
     * Either3<String,String,Integer> just  = Either3.right(10);
       Either3<String,String,Integer> none = Either3.left("none");
        
        
     * Either3<ListX<String>,ListX<String>,ListX<Integer>> xors =Either3.sequence(ListX.of(just,none,Either3.right(1)));
       //Eitehr.right(ListX.of(10,1)));
     * 
     * }</pre>
     *
     * 
     * 
     * @param Either3 Either3 to sequence
     * @return Either3 Sequenced
     */
    public static <LT1,LT2, PT> Either3<ListX<LT1>,ListX<LT2>,ListX<PT>> sequence(final CollectionX<Either3<LT1,LT2, PT>> xors) {
        return AnyM.sequence(anyMList(xors))
                   .unwrap();
    }
    /**
     * Traverse a Collection of Either3 producing an Either3 with a ListX, applying the transformation function to every
     * element in the list
     * 
     * @param xors Either3s to sequence and transform
     * @param fn Transformation function
     * @return An Either3 with a transformed list
     */
    public static <LT1,LT2, PT,R> Either3<ListX<LT1>,ListX<LT2>,ListX<R>> traverse(final CollectionX<Either3<LT1,LT2, PT>> xors, Function<? super PT, ? extends R> fn) {
        return  sequence(xors).map(l->l.map(fn));
    }
   

    /**
     *  Accumulate the results only from those Either3 which have a Right type present, using the supplied Monoid (a combining BiFunction/BinaryOperator and identity element that takes two
     * input values of the same type and returns the combined result) {@see com.aol.cyclops.Monoids }.
     * 
     * <pre>
     * {@code 
     * Either3<String,String,Integer> just  = Either3.right(10);
       Either3<String,String,Integer> none = Either3.left("none");
     *  
     *  Either3<ListX<String>,ListX<String>,Integer> xors = Either3.accumulatePrimary(Monoids.intSum,ListX.of(just,none,Either3.right(1)));
        //Either3.right(11);
     * 
     * }
     * </pre>
     * 
     * 
     * 
     * @param xors Collection of Eithers to accumulate primary values
     * @param reducer  Reducer to accumulate results
     * @return  Either3 populated with the accumulate primary operation
     */
    public static <LT1,LT2, RT> Either3<ListX<LT1>, ListX<LT2>, RT> accumulate(final Monoid<RT> reducer,final CollectionX<Either3<LT1, LT2, RT>> xors) {
        return sequence(xors).map(s -> s.reduce(reducer));
    }
 
  
    /**
     * Lazily construct a Right Either from the supplied publisher
     * <pre>
     * {@code 
     *   ReactiveSeq<Integer> stream =  ReactiveSeq.of(1,2,3);
        
         Either3<Throwable,String,Integer> future = Either3.fromPublisher(stream);
        
         //Either[1]
     * 
     * }
     * </pre>
     * @param pub Publisher to construct an Either from
     * @return Either constructed from the supplied Publisher
     */
    public static <T1,T> Either3<Throwable, T1, T> fromPublisher(final Publisher<T> pub) {
        final ValueSubscriber<T> sub = ValueSubscriber.subscriber();
        pub.subscribe(sub);
        Either3<Throwable, T1, Xor<Throwable,T>> xor = Either3.rightEval(Eval.later(()->sub.toXor()));
        return  xor.flatMap(x->x.visit(Either3::left1,Either3::right));
    }
    /**
     * Construct a Right Either3 from the supplied Iterable
     * <pre>
     * {@code 
     *   List<Integer> list =  Arrays.asList(1,2,3);
        
         Either3<Throwable,String,Integer> future = Either3.fromIterable(list);
        
         //Either[1]
     * 
     * }
     * </pre> 
     * @param iterable Iterable to construct an Either from
     * @return Either constructed from the supplied Iterable
     */
    public static <ST, T,RT> Either3<ST, T,RT> fromIterable(final Iterable<RT> iterable) {

        final Iterator<RT> it = iterable.iterator();
        return it.hasNext() ? Either3.right( it.next()) : Either3.left1(null);
    }
    
    /**
     * Static method useful as a method reference for fluent consumption of any value type stored in this Either 
     * (will capture the lowest common type)
     * 
     * <pre>
     * {@code 
     * 
     *   myEither.to(Either3::consumeAny)
                 .accept(System.out::println);
     * }
     * </pre>
     * 
     * @param either Either to consume value for
     * @return Consumer we can apply to consume value
     */
    static <X, LT extends X, M extends X, RT extends X>  Consumer<Consumer<? super X>> consumeAny(Either3<LT,M,RT> either){
        return in->visitAny(in,either);
    }
    
    static <X, LT extends X, M extends X, RT extends X,R>  Function<Function<? super X, R>,R> applyAny(Either3<LT,M,RT> either){
        return in->visitAny(either,in);
    }
    static <X, LT extends X, M extends X, RT extends X,R> R visitAny(Either3<LT,M,RT> either, Function<? super X, ? extends R> fn){
        return either.visit(fn, fn,fn);
    }
    static <X, LT extends X, M extends X, RT extends X> X visitAny(Consumer<? super X> c,Either3<LT,M,RT> either){
        Function<? super X, X> fn = x ->{
            c.accept(x);
            return x;
        };
        return visitAny(either,fn);
    }
    /**
     * Construct a Either3#Right from an Eval
     * 
     * @param right Eval to construct Either3#Right from
     * @return Either3 right instance
     */
    public static <LT, B, RT> Either3<LT, B, RT> rightEval(final Eval<RT> right) {
        return new Right<>(
                           right);
    }

    /**
     * Construct a Either3#Left1 from an Eval
     * 
     * @param left Eval to construct Either3#Left from
     * @return Either3 left instance
     */
    public static <LT, B, RT> Either3<LT, B, RT> left1Eval(final Eval<LT> left) {
        return new Left1<>(
                          left);
    }

    /**
     * Construct a Either3#Right
     * 
     * @param right Value to store
     * @return Either3 Right instance
     */
    public static <LT, B, RT> Either3<LT, B, RT> right(final RT right) {
        return new Right<>(
                           Eval.later(()->right));
    }

    /**
     * Construct a Either3#Left1
     * 
     * @param left Value to store
     * @return Left instance
     */
    public static <LT, B, RT> Either3<LT, B, RT> left1(final LT left) {
        return new Left1<>(
                          Eval.now(left));
    }

    /**
     * Construct a Either3#Left2
     * 
     * @param left2 Value to store
     * @return Left2 instance
     */
    public static <LT, B, RT> Either3<LT, B, RT> left2(final B middle) {
        return new Left2<>(
                            Eval.now(middle));
    }

    /**
     * Construct a Either3#Left2 from an Eval
     * 
     * @param middle Eval to construct Either3#middle from
     * @return Either3 Left2 instance
     */
    public static <LT, B, RT> Either3<LT, B, RT> left2Eval(final Eval<B> middle) {
        return new Left2<>(
                            middle);
    }

    /**
     * Visit the types in this Either3, only one user supplied function is executed depending on the type
     * 
     * @param left1 Function to execute if this Either3 is a Left instance
     * @param mid Function to execute if this Either3 is a middle instance
     * @param right Function to execute if this Either3 is a right instance
     * @return Result of executed function
     */
    <R> R visit(final Function<? super LT1, ? extends R> left1, final Function<? super LT2, ? extends R> mid,
            final Function<? super RT, ? extends R> right);

    /**
     * Filter this Either3 resulting in a Maybe#none if it is not a Right instance or if the predicate does not
     * hold. Otherwise results in a Maybe containing the current value
     * 
     * @param test Predicate to apply to filter this Either3
     * @return Maybe containing the current value if this is a Right instance and the predicate holds, otherwise Maybe#none
     */
    Maybe<RT> filter(Predicate<? super RT> test);

    /**
     * Flattening transformation on this Either3. Contains an internal trampoline so will convert tail-recursive calls
     * to iteration.
     * 
     * @param mapper Mapping function
     * @return Mapped Either3
     */
    <R2> Either3<LT1, LT2, R2> flatMap(Function<? super RT, ? extends MonadicValue<? extends R2>> mapper);
    

    default < RT1> Either3<LT1, LT2, RT1>  flatMapIterable(Function<? super RT, ? extends Iterable<? extends RT1>> mapper){
        return this.flatMap(a -> {
            return Either3.fromIterable(mapper.apply(a));

        });
    }
    default < RT1> Either3<LT1, LT2, RT1>  flatMapPublisher(Function<? super RT, ? extends Publisher<? extends RT1>> mapper){
        return this.flatMap(a -> {
            final Publisher<? extends RT1> publisher = mapper.apply(a);
            final ValueSubscriber<RT1> sub = ValueSubscriber.subscriber();

            publisher.subscribe(sub);
            return unit(sub.get());

        });
    }
    /**
     * @return Swap the middle and the right types
     */
    Either3<LT1, RT, LT2> swap2();

    /**
     * @return Swap the right and left types
     */
    Either3<RT, LT2, LT1> swap1();

    /**
     * @return True if this either contains the right type
     */
    boolean isRight();

    /**
     * @return True if this either contains the left1 type
     */
    boolean isLeft1();

    /**
     * @return True if this either contains the left2 type
     */
    boolean isLeft2();

    /**
     * Return an Ior that can be this object or a Ior.primary or Ior.secondary
     * @return new Ior 
     */
     default Ior<LT1, RT> toIor() {
        return this.visit(l->Ior.secondary(l), 
                          m->Ior.secondary(null),
                          r->Ior.primary(r));
    }
     default Xor<LT1, RT> toXor() {
         return this.visit(l->Xor.secondary(l), 
                           m->Xor.secondary(null),
                           r->Xor.primary(r));
     }
    
     
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#ofType(java.lang.Class)
     */
    @Override
    default <U> Maybe<U> ofType(Class<? extends U> type) {
        return (Maybe<U> )MonadicValue.super.ofType(type);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#filterNot(java.util.function.Predicate)
     */
    @Override
    default Maybe<RT> filterNot(Predicate<? super RT> predicate) {
        return (Maybe<RT>)MonadicValue.super.filterNot(predicate);
    }

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.Filterable#notNull()
     */
    @Override
    default Maybe<RT> notNull() {
        
        return (Maybe<RT>)MonadicValue.super.notNull();
    }
    
    

    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#coflatMap(java.util.function.Function)
     */
    @Override
    default <R> Either3<LT1,LT2,R> coflatMap(Function<? super MonadicValue<RT>, R> mapper) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.coflatMap(mapper);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#nest()
     */
    @Override
    default Either3<LT1,LT2,MonadicValue<RT>> nest() {
       
        return (Either3<LT1,LT2,MonadicValue<RT>>)MonadicValue.super.nest();
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#forEach4(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops.util.function.TriFunction, com.aol.cyclops.util.function.QuadFunction)
     */
    @Override
    default <T2, R1, R2, R3, R> Either3<LT1,LT2,R> forEach4(Function<? super RT, ? extends MonadicValue<R1>> value1,
            BiFunction<? super RT, ? super R1, ? extends MonadicValue<R2>> value2,
            TriFunction<? super RT, ? super R1, ? super R2, ? extends MonadicValue<R3>> value3,
            QuadFunction<? super RT, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.forEach4(value1, value2, value3, yieldingFunction);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#forEach4(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops.util.function.TriFunction, com.aol.cyclops.util.function.QuadFunction, com.aol.cyclops.util.function.QuadFunction)
     */
    @Override
    default <T2, R1, R2, R3, R> Either3<LT1,LT2,R> forEach4(Function<? super RT, ? extends MonadicValue<R1>> value1,
            BiFunction<? super RT, ? super R1, ? extends MonadicValue<R2>> value2,
            TriFunction<? super RT, ? super R1, ? super R2, ? extends MonadicValue<R3>> value3,
            QuadFunction<? super RT, ? super R1, ? super R2, ? super R3, Boolean> filterFunction,
            QuadFunction<? super RT, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.forEach4(value1, value2, value3, filterFunction, yieldingFunction);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#forEach3(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops.util.function.TriFunction)
     */
    @Override
    default <T2, R1, R2, R> Either3<LT1,LT2,R> forEach3(Function<? super RT, ? extends MonadicValue<R1>> value1,
            BiFunction<? super RT, ? super R1, ? extends MonadicValue<R2>> value2,
            TriFunction<? super RT, ? super R1, ? super R2, ? extends R> yieldingFunction) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.forEach3(value1, value2, yieldingFunction);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#forEach3(java.util.function.Function, java.util.function.BiFunction, com.aol.cyclops.util.function.TriFunction, com.aol.cyclops.util.function.TriFunction)
     */
    @Override
    default <T2, R1, R2, R> Either3<LT1,LT2,R> forEach3(Function<? super RT, ? extends MonadicValue<R1>> value1,
            BiFunction<? super RT, ? super R1, ? extends MonadicValue<R2>> value2,
            TriFunction<? super RT, ? super R1, ? super R2, Boolean> filterFunction,
            TriFunction<? super RT, ? super R1, ? super R2, ? extends R> yieldingFunction) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.forEach3(value1, value2, filterFunction, yieldingFunction);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#forEach2(java.util.function.Function, java.util.function.BiFunction)
     */
    @Override
    default <R1, R> Either3<LT1,LT2,R> forEach2(Function<? super RT, ? extends MonadicValue<R1>> value1,
            BiFunction<? super RT, ? super R1, ? extends R> yieldingFunction) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.forEach2(value1, yieldingFunction);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#forEach2(java.util.function.Function, java.util.function.BiFunction, java.util.function.BiFunction)
     */
    @Override
    default <R1, R> Either3<LT1,LT2,R> forEach2(Function<? super RT, ? extends MonadicValue<R1>> value1,
            BiFunction<? super RT, ? super R1, Boolean> filterFunction,
            BiFunction<? super RT, ? super R1, ? extends R> yieldingFunction) {
       
        return (Either3<LT1,LT2,R>)MonadicValue.super.forEach2(value1, filterFunction, yieldingFunction);
    }
    /* (non-Javadoc)
     * @see com.aol.cyclops.types.MonadicValue#combineEager(com.aol.cyclops.Monoid, com.aol.cyclops.types.MonadicValue)
     */
    @Override
    default Either3<LT1,LT2,RT> combineEager(Monoid<RT> monoid, MonadicValue<? extends RT> v2) {
       
        return (Either3<LT1,LT2,RT>)MonadicValue.super.combineEager(monoid, v2);
    }
    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.BiFunctor#bimap(java.util.function.Function,
     * java.util.function.Function)
     */
    @Override
    <R1, R2> Either3<LT1, R1, R2> bimap(Function<? super LT2, ? extends R1> fn1, Function<? super RT, ? extends R2> fn2);

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Functor#map(java.util.function.Function)
     */
    @Override
    <R> Either3<LT1, LT2, R> map(Function<? super RT, ? extends R> fn);

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Combiner#combine(com.aol.cyclops.types.Value,
     * java.util.function.BiFunction)
     */
    @Override
    default <T2, R> Either3<LT1, LT2, R> combine(final Value<? extends T2> app,
            final BiFunction<? super RT, ? super T2, ? extends R> fn) {

        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.combine(app, fn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.Combiner#combine(java.util.function.BinaryOperator,
     * com.aol.cyclops.types.Combiner)
     */
    @Override
    default Either3<LT1, LT2, RT> combine(final BinaryOperator<Combiner<RT>> combiner, final Combiner<RT> app) {

        return (Either3<LT1, LT2, RT>) ApplicativeFunctor.super.combine(combiner, app);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(org.jooq.lambda.Seq,
     * java.util.function.BiFunction)
     */
    @Override
    default <U, R> Either3<LT1, LT2, R> zip(final Seq<? extends U> other,
            final BiFunction<? super RT, ? super U, ? extends R> zipper) {
        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(java.util.stream.Stream,
     * java.util.function.BiFunction)
     */
    @Override
    default <U, R> Either3<LT1, LT2, R> zip(final Stream<? extends U> other,
            final BiFunction<? super RT, ? super U, ? extends R> zipper) {

        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.zip(other, zipper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(java.util.stream.Stream)
     */
    @Override
    default <U> Either3<LT1, LT2, Tuple2<RT, U>> zip(final Stream<? extends U> other) {

        return (Either3) ApplicativeFunctor.super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(org.jooq.lambda.Seq)
     */
    @Override
    default <U> Either3<LT1, LT2, Tuple2<RT, U>> zip(final Seq<? extends U> other) {

        return (Either3) ApplicativeFunctor.super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Zippable#zip(java.lang.Iterable)
     */
    @Override
    default <U> Either3<LT1, LT2, Tuple2<RT, U>> zip(final Iterable<? extends U> other) {

        return (Either3) ApplicativeFunctor.super.zip(other);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Unit#unit(java.lang.Object)
     */
    @Override
    <T> Either3<LT1, LT2, T> unit(T unit);

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.lang.
     * Iterable, java.util.function.BiFunction)
     */
    @Override
    default <T2, R> Either3<LT1, LT2, R> zip(final Iterable<? extends T2> app,
            final BiFunction<? super RT, ? super T2, ? extends R> fn) {

        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.zip(app, fn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#zip(java.util.
     * function.BiFunction, org.reactivestreams.Publisher)
     */
    @Override
    default <T2, R> Either3<LT1, LT2, R> zip(final BiFunction<? super RT, ? super T2, ? extends R> fn,
            final Publisher<? extends T2> app) {

        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.zip(fn, app);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.BiFunctor#bipeek(java.util.function.Consumer,
     * java.util.function.Consumer)
     */
    @Override
    default Either3<LT1, LT2, RT> bipeek(final Consumer<? super LT2> c1, final Consumer<? super RT> c2) {

        return (Either3<LT1, LT2, RT>) BiFunctor.super.bipeek(c1, c2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.BiFunctor#bicast(java.lang.Class,
     * java.lang.Class)
     */
    @Override
    default <U1, U2> Either3<LT1, U1, U2> bicast(final Class<U1> type1, final Class<U2> type2) {

        return (Either3<LT1, U1, U2>) BiFunctor.super.bicast(type1, type2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.BiFunctor#bitrampoline(java.util.function.Function,
     * java.util.function.Function)
     */
    @Override
    default <R1, R2> Either3<LT1, R1, R2> bitrampoline(
            final Function<? super LT2, ? extends Trampoline<? extends R1>> mapper1,
            final Function<? super RT, ? extends Trampoline<? extends R2>> mapper2) {

        return (Either3<LT1, R1, R2>) BiFunctor.super.bitrampoline(mapper1, mapper2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Functor#cast(java.lang.Class)
     */
    @Override
    default <U> Either3<LT1, LT2, U> cast(final Class<? extends U> type) {

        return (Either3<LT1, LT2, U>) ApplicativeFunctor.super.cast(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.cyclops.types.Functor#peek(java.util.function.Consumer)
     */
    @Override
    default Either3<LT1, LT2, RT> peek(final Consumer<? super RT> c) {

        return (Either3<LT1, LT2, RT>) ApplicativeFunctor.super.peek(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.Functor#trampoline(java.util.function.Function)
     */
    @Override
    default <R> Either3<LT1, LT2, R> trampoline(final Function<? super RT, ? extends Trampoline<? extends R>> mapper) {

        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.trampoline(mapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.types.Functor#patternMatch(java.util.function.Function,
     * java.util.function.Supplier)
     */
    @Override
    default <R> Either3<LT1, LT2, R> patternMatch(final Function<CheckValue1<RT, R>, CheckValue1<RT, R>> case1,
            final Supplier<? extends R> otherwise) {

        return (Either3<LT1, LT2, R>) ApplicativeFunctor.super.patternMatch(case1, otherwise);
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final static class Lazy<ST, M, PT> implements Either3<ST, M, PT> {

        private final Eval<Either3<ST, M, PT>> lazy;

        public Either3<ST, M, PT> resolve() {
            return lazy.get()
                       .visit(Either3::left1, Either3::left2, Either3::right);
        }

        private static <ST, M, PT> Lazy<ST, M, PT> lazy(final Eval<Either3<ST, M, PT>> lazy) {
            return new Lazy<>(
                              lazy);
        }

        @Override
        public <R> Either3<ST, M, R> map(final Function<? super PT, ? extends R> mapper) {

            return lazy(Eval.later(() -> resolve().map(mapper)));

        }

        @Override
        public <RT1> Either3<ST, M, RT1> flatMap(
                final Function<? super PT, ? extends MonadicValue<? extends RT1>> mapper) {

            return lazy(Eval.later(() -> resolve().flatMap(mapper)));
      

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.fromEval(Eval.later(() -> resolve().filter(test)))
                        .flatMap(Function.identity());

        }

        @Override
        public PT get() {
            return trampoline().get();
        }

        private Either3<ST,M,PT> trampoline(){
            Either3<ST,M,PT> maybe = lazy.get();
            while (maybe instanceof Lazy) {
                maybe = ((Lazy<ST,M,PT>) maybe).lazy.get();
            }
            return maybe;
        }
        @Override
        public ReactiveSeq<PT> stream() {

            return trampoline()
                       .stream();
        }

        @Override
        public Iterator<PT> iterator() {

            return trampoline()
                       .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {

            return trampoline()
                       .visit(present, absent);
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

            lazy.get()
                .subscribe(s);
        }

        @Override
        public boolean test(final PT t) {
            return trampoline()
                       .test(t);
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid, final Function<? super PT, ? extends R> primary) {

            return trampoline()
                       .visit(secondary, mid, primary);
        }

        @Override
        public Either3<ST, PT, M> swap2() {
            return lazy(Eval.later(() -> resolve().swap2()));
        }

        @Override
        public Either3<PT, M, ST> swap1() {
            return lazy(Eval.later(() -> resolve().swap1()));
        }

        @Override
        public boolean isRight() {
            return trampoline()
                       .isRight();
        }

        @Override
        public boolean isLeft1() {
            return trampoline()
                       .isLeft1();
        }

        @Override
        public boolean isLeft2() {
            return trampoline()
                       .isLeft2();
        }

        @Override
        public <R1, R2> Either3<ST, R1, R2> bimap(final Function<? super M, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return lazy(Eval.later(() -> resolve().bimap(fn1, fn2)));
        }

        @Override
        public <T> Either3<ST, M, T> unit(final T unit) {

            return Either3.right(unit);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.visit(Either3::left1,Either3::left2,Either3::right).hashCode();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
           return this.visit(Either3::left1,Either3::left2,Either3::right).equals(obj);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return trampoline().toString();
        }
        

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class Right<ST, M, PT> implements Either3<ST, M, PT> {
        private final Eval<PT> value;

        @Override
        public <R> Either3<ST, M, R> map(final Function<? super PT, ? extends R> fn) {
            return new Right<ST, M, R>(
                                       value.map(fn));
        }

        @Override
        public Either3<ST, M, PT> peek(final Consumer<? super PT> action) {
            return map(i -> {
                action.accept(i);
                return i;
            });

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.fromEval(Eval.later(() -> test.test(get()) ? Maybe.just(get()) : Maybe.<PT> none()))
                        .flatMap(Function.identity());

        }

        @Override
        public PT get() {
            return value.get();
        }

        @Override
        public <RT1> Either3<ST, M, RT1> flatMap(
                final Function<? super PT, ? extends MonadicValue<? extends RT1>> mapper) {
            
            Eval<? extends Either3<? extends ST, ? extends M, ? extends RT1>> et = value.map(mapper.andThen(Either3::fromMonadicValue));
             
             
            final Eval<Either3<ST, M, RT1>> e3 =  (Eval<Either3<ST, M, RT1>>)et;
            return new Lazy<>(
                              e3);
          

        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public boolean isLeft1() {
            return false;
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either3.right[" + value.get() + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid, final Function<? super PT, ? extends R> primary) {
            return primary.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either3<ST, M, R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return new Right<>(
                               value.combine(app, fn));

        }

        @Override
        public <R1, R2> Either3<ST, R1, R2> bimap(final Function<? super M, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either3<ST, R1, R2>) this.map(fn2);
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return value.stream();
        }

        @Override
        public Iterator<PT> iterator() {
            return value.iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return value.visit(present, absent);
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {
            value.subscribe(s);

        }

        @Override
        public boolean test(final PT t) {
            return value.test(t);
        }

        @Override
        public <T> Either3<ST, M, T> unit(final T unit) {
            return Either3.right(unit);
        }

        @Override
        public Either3<ST, PT, M> swap2() {

            return new Left2<>(value);
        }

        @Override
        public Either3<PT, M, ST> swap1() {

            return new Left1<>(
                              value);
        }

        @Override
        public boolean isLeft2() {

            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if(obj instanceof Lazy){
                return ((Lazy)obj).equals(this);
            }
            if (getClass() != obj.getClass())
                return false;
            Right other = (Right) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
        

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class Left1<ST, M, PT> implements Either3<ST, M, PT> {
        private final Eval<ST> value;

        @Override
        public <R> Either3<ST, M, R> map(final Function<? super PT, ? extends R> fn) {
            return (Either3<ST, M, R>) this;
        }

        @Override
        public Either3<ST, M, PT> peek(final Consumer<? super PT> action) {
            return this;

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.none();

        }

        @Override
        public PT get() {
            throw new NoSuchElementException(
                                             "Attempt to access right value on a Left Either3");
        }

        @Override
        public <RT1> Either3<ST, M, RT1> flatMap(
                final Function<? super PT, ? extends MonadicValue<? extends RT1>> mapper) {

            return (Either3) this;

        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public boolean isLeft1() {
            return true;
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either3.left1[" + value.get() + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid, final Function<? super PT, ? extends R> primary) {
            return secondary.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either3<ST, M, R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return (Either3<ST, M, R>) this;

        }

        @Override
        public <R1, R2> Either3<ST, R1, R2> bimap(final Function<? super M, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either3<ST, R1, R2>) this;
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return ReactiveSeq.empty();
        }

        @Override
        public Iterator<PT> iterator() {
            return Arrays.<PT> asList()
                         .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return absent.get();
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

        }

        @Override
        public boolean test(final PT t) {
            return false;
        }

        @Override
        public <T> Either3<ST, M, T> unit(final T unit) {
            return Either3.right(unit);
        }

        @Override
        public Either3<ST, PT, M> swap2() {

            return (Either3<ST, PT, M>) this;
        }

        @Override
        public Either3<PT, M, ST> swap1() {

            return new Right<>(
                               value);
        }

        @Override
        public boolean isLeft2() {

            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if(obj instanceof Lazy){
                return ((Lazy)obj).equals(this);
            }
            if (getClass() != obj.getClass())
                return false;
            Left1 other = (Left1) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class Left2<ST, M, PT> implements Either3<ST, M, PT> {
        private final Eval<M> value;

        @Override
        public <R> Either3<ST, M, R> map(final Function<? super PT, ? extends R> fn) {
            return (Either3<ST, M, R>) this;
        }

        @Override
        public Either3<ST, M, PT> peek(final Consumer<? super PT> action) {
            return this;

        }

        @Override
        public Maybe<PT> filter(final Predicate<? super PT> test) {

            return Maybe.none();

        }

        @Override
        public PT get() {
            throw new NoSuchElementException(
                                             "Attempt to access right value on a Middle Either3");
        }

        @Override
        public <RT1> Either3<ST, M, RT1> flatMap(
                final Function<? super PT, ? extends MonadicValue<? extends RT1>> mapper) {

            return (Either3) this;

        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public boolean isLeft1() {
            return false;
        }

        @Override
        public String toString() {
            return mkString();
        }

        @Override
        public String mkString() {
            return "Either3.left2[" + value.get() + "]";
        }

        @Override
        public <R> R visit(final Function<? super ST, ? extends R> secondary,
                final Function<? super M, ? extends R> mid, final Function<? super PT, ? extends R> primary) {
            return mid.apply(value.get());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.aol.cyclops.types.applicative.ApplicativeFunctor#ap(com.aol.
         * cyclops.types.Value, java.util.function.BiFunction)
         */
        @Override
        public <T2, R> Either3<ST, M, R> combine(final Value<? extends T2> app,
                final BiFunction<? super PT, ? super T2, ? extends R> fn) {
            return (Either3<ST, M, R>) this;

        }

        @Override
        public <R1, R2> Either3<ST, R1, R2> bimap(final Function<? super M, ? extends R1> fn1,
                final Function<? super PT, ? extends R2> fn2) {
            return (Either3<ST, R1, R2>) this;
        }

        @Override
        public ReactiveSeq<PT> stream() {
            return ReactiveSeq.empty();
        }

        @Override
        public Iterator<PT> iterator() {
            return Arrays.<PT> asList()
                         .iterator();
        }

        @Override
        public <R> R visit(final Function<? super PT, ? extends R> present, final Supplier<? extends R> absent) {
            return absent.get();
        }

        @Override
        public void subscribe(final Subscriber<? super PT> s) {

        }

        @Override
        public boolean test(final PT t) {
            return false;
        }

        @Override
        public <T> Either3<ST, M, T> unit(final T unit) {
            return Either3.right(unit);
        }

        @Override
        public Either3<ST, PT, M> swap2() {
            return new Right<>(
                               value);

        }

        @Override
        public Either3<PT, M, ST> swap1() {
            return (Either3<PT, M, ST>) this;

        }

        @Override
        public boolean isLeft2() {

            return true;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            if(obj instanceof Lazy){
                return ((Lazy)obj).equals(this);
            }
            Left2 other = (Left2) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }

    }

}
