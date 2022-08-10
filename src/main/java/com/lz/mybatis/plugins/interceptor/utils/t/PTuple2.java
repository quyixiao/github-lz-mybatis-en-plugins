package com.lz.mybatis.plugins.interceptor.utils.t;

public class PTuple2<A, B> extends PTuple1<A> {
    private B second;

    public PTuple2(A a, B b) {
        super(a);
        second = b;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

}

