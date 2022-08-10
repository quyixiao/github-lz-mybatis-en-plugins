package com.lz.mybatis.plugins.interceptor.utils.t;


/***
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class PTuple3<A, B, C> extends PTuple2<A, B> {
    private C third;

    public PTuple3(A a, B b, C c) {
        super(a, b);
        third = c;
    }


    public C getThird() {
        return third;
    }

    public void setThird(C third) {
        this.third = third;
    }
}
