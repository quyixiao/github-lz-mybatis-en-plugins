//: net/mindview/util/FourTuple.java
package com.lz.mybatis.plugins.interceptor.utils.t;


/***
 *
 *
 * 355页
 *
 *
 * @param <A>
 * @param <B>
 * @param <C>
 * @param <D>
 */
public class PTuple4<A, B, C, D> extends PTuple3<A, B, C> {
    private D fourth;

    public PTuple4(A a, B b, C c, D d) {
        super(a, b, c);
        fourth = d;
    }


    public D getFourth() {
        return fourth;
    }

    public void setFourth(D fourth) {
        this.fourth = fourth;
    }
}
