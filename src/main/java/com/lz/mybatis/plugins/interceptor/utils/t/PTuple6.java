package com.lz.mybatis.plugins.interceptor.utils.t;


/******
 *
 *
 *
 * @param <A>
 * @param <B>
 * @param <C>
 * @param <D>
 * @param <E>
 * @param <F>
 */
public class PTuple6<A, B, C, D, E, F> extends PTuple5<A, B, C, D, E> {
    private F sixth;

    public PTuple6(A a, B b, C c, D d, E e, F f) {
        super(a, b, c, d, e);
        sixth = f;
    }


    public F getSixth() {
        return sixth;
    }

    public void setSixth(F sixth) {
        this.sixth = sixth;
    }

    static PTuple6<String, String, String, Float, Double, Byte> a() {
        return new PTuple6<String, String, String, Float, Double, Byte>(
                "11111", "", "hi", (float) 4.7,
                1.1, (byte) 1);
    }


}
