package com.lz.mybatis.plugins.interceptor.utils.t;

import java.io.Serializable;

public class PTuple10<A, B, C, D, E, F, G, H, I, J> extends PTuple9<A, B, C, D, E, F, G, H, I> implements Serializable {

    private J ten;

    public PTuple10(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j) {
        super(a, b, c, d, e, f, g, h, i);
        ten = j;
    }

    public J getTen() {
        return ten;
    }

    public void setTen(J ten) {
        this.ten = ten;
    }


}
