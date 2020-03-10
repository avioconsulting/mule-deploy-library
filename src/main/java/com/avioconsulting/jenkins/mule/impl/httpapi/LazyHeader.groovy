package com.avioconsulting.jenkins.mule.impl.httpapi

import groovy.transform.Memoized
import org.apache.http.message.BasicHeader

class LazyHeader extends BasicHeader {
    private final Closure lazyEvaluator

    LazyHeader(String name,
               Closure lazyEvaluator) {
        super(name,
              'not_evaluated_yet')
        this.lazyEvaluator = lazyEvaluator
    }

    @Override
    @Memoized
    String getValue() {
        lazyEvaluator()
    }
}
