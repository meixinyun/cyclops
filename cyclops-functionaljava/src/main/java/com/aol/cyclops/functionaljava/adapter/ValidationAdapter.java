package com.aol.cyclops.functionaljava.adapter;


import com.aol.cyclops.functionaljava.FJ;
import com.aol.cyclops.functionaljava.FJWitness.validation;
import com.aol.cyclops.functionaljava.FromCyclopsReact;
import com.aol.cyclops.functionaljava.ToCyclopsReact;
import com.aol.cyclops2.types.extensability.AbstractFunctionalAdapter;
import cyclops.control.Maybe;
import cyclops.control.Xor;
import cyclops.monads.AnyM;
import fj.data.Validation;
import lombok.AllArgsConstructor;

import java.util.function.Function;
import java.util.function.Predicate;


@AllArgsConstructor
public class ValidationAdapter<L> extends AbstractFunctionalAdapter<validation> {



    @Override
    public <T> Iterable<T> toIterable(AnyM<validation, T> t) {
        return Maybe.fromIterable(t);
    }

    @Override
    public <T, R> AnyM<validation, R> ap(AnyM<validation,? extends Function<? super T,? extends R>> fn, AnyM<validation, T> apply) {
        Validation<L,T> f = validation(apply);
        Validation<L,? extends Function<? super T, ? extends R>> fnF = validation(fn);
        Validation<L,R> res = FromCyclopsReact.validation(ToCyclopsReact.xor(fnF).combine(ToCyclopsReact.xor(f), (a, b) -> a.apply(b)));
        return FJ.validation(res);

    }

    @Override
    public <T> AnyM<validation, T> filter(AnyM<validation, T> t, Predicate<? super T> fn) {
        return t;
    }

    <T> Validation<L,T> validation(AnyM<validation,T> anyM){
        return anyM.unwrap();
    }

    @Override
    public <T> AnyM<validation, T> empty() {
        return FJ.validation(Validation.fail(null));
    }



    @Override
    public <T, R> AnyM<validation, R> flatMap(AnyM<validation, T> t,
                                     Function<? super T, ? extends AnyM<validation, ? extends R>> fn) {
        return FJ.validation(validation(t).bind(a-> validation((AnyM<validation,R>)fn.apply(a))));

    }

    @Override
    public <T> AnyM<validation, T> unitIterable(Iterable<T> it)  {

        return FJ.validation(FromCyclopsReact.validation(Xor.fromIterable(it)));
    }

    @Override
    public <T> AnyM<validation, T> unit(T o) {
        return FJ.validation(Validation.success(o));
    }



}