package com.lz.mybatis.plugins.interceptor.utils.t;

public class PTuple8<A, B, C, D, E, F, G, H> extends PTuple7<A, B, C, D, E, F, G> {


    private H eight;

    public PTuple8(A a, B b, C c, D d, E e, F f, G g, H h) {
        super(a, b, c, d, e, f, g);
        eight = h;
    }


    public H getEight() {
        return eight;
    }

    public void setEight(H eight) {
        this.eight = eight;
    }
}
