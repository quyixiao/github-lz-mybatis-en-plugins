/**
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.lz.mybatis.plugins.interceptor.baomidou.plugins;

import com.lz.mybatis.plugins.interceptor.baomidou.entity.SqlInfo;
import org.apache.ibatis.reflection.MetaObject;

/**
 * <p>
 * SQL 解析接口
 * </p>
 *
 * @author hubin
 * @Date 2017-09-01
 */
public interface ISqlParser {

    /**
     * <p>
     * 获取优化 SQL 方法
     * </p>
     *
     * @param metaObject 元对象
     * @param sql        SQL 语句
     * @return SQL 信息
     */
    SqlInfo optimizeSql(MetaObject metaObject, String sql);

}
