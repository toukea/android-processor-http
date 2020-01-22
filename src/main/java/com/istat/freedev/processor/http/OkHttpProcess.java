package com.istat.freedev.processor.http;

import com.google.gson.Gson;
import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.interfaces.ProcessCallback;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import istat.android.base.tools.Reflections;
import istat.android.base.tools.ToolKits;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class OkHttpProcess<Result> extends Process<Result, OkHttpProcess.Error> {
    Call call;

    @Override
    protected final void onExecute(ExecutionVariables executionVariables) throws Exception {
        //TODO pour prendre en compte des cas generique, il faudrat que cette part puisse être custom dans une method abstract.
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        onPrepareClient(builder, executionVariables);
        OkHttpClient client = builder.build();
        Request.Builder requestBuilder = new Request.Builder();
        onPrepareRequest(requestBuilder, executionVariables);
        requestBuilder.tag(this);
        Request request = requestBuilder.build();
        call = client.newCall(request);
        enqueueRequest();
    }

    protected void enqueueRequest() {
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!onHttpFailure(call, e)) {
                    notifyFailed(e);
                }
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (!onHttpResponse(call, response)) {
                    if (response.isSuccessful()) {
                        Result result = onCreateResultResponse(response);
                        notifySucceed(result);
                    } else {
                        Error error = new Error(response);
                        notifyError(error);
                    }
                }
            }
        });
    }

    protected boolean onHttpResponse(Call call, okhttp3.Response response) {
        return false;
    }

    protected boolean onHttpFailure(Call call, IOException e) {
        return false;
    }

    @Override
    protected void onCancel() {
        call.cancel();
    }

    @Override
    public boolean isCanceled() {
        return call != null && call.isCanceled();
    }

    protected void onPrepareClient(OkHttpClient.Builder builder, ExecutionVariables executionVariables) {

    }

    protected abstract void onPrepareRequest(Request.Builder builder, ExecutionVariables executionVariables);
//    {
//        String method = executionVariables.getStringVariable(0);
//        String uri = executionVariables.getStringVariable(1);
//        Object body = executionVariables.getVariable(2);
//        HashMap<String, String> headers = executionVariables.getVariable(3);
//
//    }

    protected Result onCreateResultResponse(okhttp3.Response response) throws IOException {
        String jsonString = ToolKits.Stream.streamToString(response.body().byteStream());
        Gson gson = new Gson();
        Type type = Reflections.getGenericType(this.getClass(), 0);
        Result result = gson.fromJson(jsonString, type);
        return result;
    }

    public static class Error extends Exception {
        Response response;

        Error(okhttp3.Response response) {
            super(response.message());
            this.response = response;

        }

        public String getHeader(String header) {
            return response.header(header);
        }

        public List<String> getHeaders(String header) {
            return response.headers(header);
        }

        public int getCode() {
            return response.code();
        }
    }

    public interface Callback<T> extends ProcessCallback<T, Error> {

    }
}
