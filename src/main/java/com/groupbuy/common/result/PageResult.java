package com.groupbuy.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long pageNo;
    private long pageSize;
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> converter) {
        if (page == null || page.getRecords() == null) {
            return new PageResult<>(Collections.emptyList(), 0, 1, 10, 0);
        }
        return new PageResult<>(page.getRecords().stream().map(converter).toList(),
                page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }
}
