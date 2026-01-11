package com.ndh.ShopTechnology.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultPagination {
    private Collection<?> lst;
    private Integer total;
    private Integer begin;
    private Integer nbPerPage;
    private Integer next;

    public ResultPagination(Collection<?> lst, Integer total, Integer begin, Integer nbPerPage) {
        this.lst = lst;
        this.total = total;
        this.begin = begin;
        this.nbPerPage = nbPerPage;
        this.next = (begin + nbPerPage) < total ? (begin + nbPerPage) : null;
    }

}
