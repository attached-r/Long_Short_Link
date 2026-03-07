package rj.highlink.common.result;

import lombok.Data;

/**
 * 统一API响应结果
 * @param <T> 数据泛型
 */
@Data
public class R<T> {

    /**
     * 状态码：200=成功，其他=失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    // 成功静态方法

    // 不含 数据
    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("操作成功");
        return r;
    }
    // 传入数据
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("操作成功");
        r.setData(data);
        return r;
    }
    // 传入信息和 数据
    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    // 失败静态方法
    public static <T> R<T> fail() {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMessage("操作失败");
        return r;
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
    // 重写toString，避免误用
    @Override
    public String toString() {
        return String.format("R{code=%d, message='%s', data=%s}", code, message, data);
    }
}
