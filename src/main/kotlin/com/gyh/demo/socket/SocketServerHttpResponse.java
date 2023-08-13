package com.gyh.demo.socket;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * create by GYH on 2023/8/11
 */
public class SocketServerHttpResponse extends AbstractServerHttpResponse {

    private Flux<DataBuffer> body = Flux.error(new IllegalStateException(
            "No content was written nor was setComplete() called on this response."));

    private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;
    private Integer req;

    public SocketServerHttpResponse() {
        this(DefaultDataBufferFactory.sharedInstance);
    }

    public SocketServerHttpResponse(DataBufferFactory dataBufferFactory) {
        super(dataBufferFactory);
        this.writeHandler = body -> {
            // Avoid .then() that causes data buffers to be discarded and released
            Sinks.Empty<Void> completion = Sinks.unsafe().empty();
            this.body = body.cache();
            this.body.subscribe(aVoid -> {
            }, completion::tryEmitError, completion::tryEmitEmpty); // Signals are serialized
            return completion.asMono();
        };
    }


    /**
     * Configure a custom handler to consume the response body.
     * <p>By default, response body content is consumed in full and cached for
     * subsequent access in tests. Use this option to take control over how the
     * response body is consumed.
     *
     * @param writeHandler the write handler to use returning {@code Mono<Void>}
     *                     when the body has been "written" (i.e. consumed).
     */
    public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
        Assert.notNull(writeHandler, "'writeHandler' is required");
        this.body = Flux.error(new IllegalStateException("Not available with custom write handler."));
        this.writeHandler = writeHandler;
    }

    public Integer getReq() {
        return req;
    }

    public void setReq(Integer req) {
        this.req = req;
    }

    @Override
    public <T> T getNativeResponse() {
        throw new IllegalStateException("This is a mock. No running server, no native response.");
    }


    @Override
    protected void applyStatusCode() {
    }

    @Override
    protected void applyHeaders() {
    }

    @Override
    protected void applyCookies() {
        for (List<ResponseCookie> cookies : getCookies().values()) {
            for (ResponseCookie cookie : cookies) {
                getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
            }
        }
    }

    @Override
    protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
        return this.writeHandler.apply(Flux.from(body));
    }

    @Override
    protected Mono<Void> writeAndFlushWithInternal(
            Publisher<? extends Publisher<? extends DataBuffer>> body) {

        return this.writeHandler.apply(Flux.from(body).concatMap(Flux::from));
    }

    @Override
    public Mono<Void> setComplete() {
        return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.empty())));
    }

    /**
     * Return the response body or an error stream if the body was not set.
     */
    public Flux<DataBuffer> getBody() {
        return this.body;
    }

    /**
     * Aggregate response data and convert to a String using the "Content-Type"
     * charset or "UTF-8" by default.
     */
    public Mono<String> getBodyAsString() {

        Charset charset = Optional.ofNullable(getHeaders().getContentType()).map(MimeType::getCharset)
                .orElse(StandardCharsets.UTF_8);

        return DataBufferUtils.join(getBody())
                .map(buffer -> {
                    String s = buffer.toString(charset);
                    DataBufferUtils.release(buffer);
                    return s;
                })
                .defaultIfEmpty("");
    }

}
