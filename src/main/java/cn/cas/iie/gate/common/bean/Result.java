package cn.cas.iie.gate.common.bean;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Result success(Object r) {
        Result ret = new Result();
        ret.setData(r);
        ret.setCode(200);
        ret.setMessage("SUCCESS");

        return ret;
    }
}
