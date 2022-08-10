package com.lz.mybatis.plugins.interceptor.utils.t;

public class PTuple7<A, B, C, D, E, F, G> extends PTuple6<A, B, C, D, E, F> {

    private G seven;

    public PTuple7(A a, B b, C c, D d, E e, F f, G g) {
        super(a, b, c, d, e, f);
        seven = g;
    }

    public G getSeven() {
        return seven;
    }

    public void setSeven(G seven) {
        this.seven = seven;
    }
}
