package com.lz.mybatis.plugins.interceptor.utils.t;

import java.io.Serializable;

public class PTuple11<A, B, C, D, E, F, G, H, I, J, K> extends PTuple10<A, B, C, D, E, F, G, H, I, J> implements Serializable {

    private K elevent;

    public PTuple11(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k) {
        super(a, b, c, d, e, f, g, h, i, j);
        elevent = k;
    }

    public K getElevent() {
        return elevent;
    }

    public void setElevent(K elevent) {
        this.elevent = elevent;
    }

}
