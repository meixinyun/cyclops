package com.aol.cyclops.vavr.adapter;


import com.aol.cyclops.vavr.FromCyclopsReact;
import com.aol.cyclops.vavr.ToCyclopsReact;
import com.aol.cyclops.vavr.Vavr;
import com.aol.cyclops.vavr.VavrWitness.tryType;
import com.aol.cyclops2.types.extensability.AbstractFunctionalAdapter;
import cyclops.control.Maybe;
import cyclops.monads.AnyM;
import javaslang.control.Try;
import lombok.AllArgsConstructor;

import java.util.function.Function;
import java.util.function.Predicate;


@AllArgsConstructor
public class TryAdapter extends AbstractFunctionalAdapter<tryType> {



    @Override
    public <T> Iterable<T> toIterable(AnyM<tryType, T> t) {
        return Maybe.fromIterable(t);
    }

    @Override
    public <T, R> AnyM<tryType, R> ap(AnyM<tryType,? extends Function<? super T,? extends R>> fn, AnyM<tryType, T> apply) {
        Try<T> f = tryType(apply);
        Try<? extends Function<? super T, ? extends R>> fnF = tryType(fn);
        Try<R> res = FromCyclopsReact.toTry(ToCyclopsReact.toTry(fnF).combine(ToCyclopsReact.toTry(f), (a, b) -> a.apply(b)));
        return Vavr.tryM(res);

    }

    @Override
    public <T> AnyM<tryType, T> filter(AnyM<tryType, T> t, Predicate<? super T> fn) {
        return Vavr.tryM(tryType(t).filter(fn));
    }

    <T> Try<T> tryType(AnyM<tryType,T> anyM){
        return anyM.unwrap();
    }

    @Override
    public <T> AnyM<tryType, T> empty() {
        return Vavr.tryM(Try.failure(null));
    }



    @Override
    public <T, R> AnyM<tryType, R> flatMap(AnyM<tryType, T> t,
                                     Function<? super T, ? extends AnyM<tryType, ? extends R>> fn) {
        return Vavr.tryM(tryType(t).flatMap(fn.andThen(a-> tryType(a))));

    }

    @Override
    public <T> AnyM<tryType, T> unitIterable(Iterable<T> it)  {
        return Vavr.tryM(FromCyclopsReact.toTry(cyclops.control.Try.fromIterable(it)));
    }

    @Override
    public <T> AnyM<tryType, T> unit(T o) {
        return Vavr.tryM(Try.success(o));
    }



}