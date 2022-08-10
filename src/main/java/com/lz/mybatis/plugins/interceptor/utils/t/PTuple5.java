//: net/mindview/util/FiveTuple.java
package com.lz.mybatis.plugins.interceptor.utils.t;


/*****
 *
 * 增加类型参数是一件简单的事情
 *
 * @param <A>
 * @param <B>
 * @param <C>
 * @param <D>
 * @param <E>
 */
public class PTuple5<A, B, C, D, E> extends PTuple4<A, B, C, D> {
    private E fifth;

    public PTuple5(A a, B b, C c, D d, E e) {
        super(a, b, c, d);
        fifth = e;
    }

    public E getFifth() {
        return fifth;
    }

    public void setFifth(E fifth) {
        this.fifth = fifth;
    }
}


///:~
