package com.lz.mybatis.plugins.interceptor.utils.t;

import java.io.Serializable;

public class PTuple1<A> implements Serializable {
    private A first;

    public PTuple1(A a) {
        first = a;
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }


}

