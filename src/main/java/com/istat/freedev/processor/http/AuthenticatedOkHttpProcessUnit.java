package com.istat.freedev.processor.http;

import com.istat.freedev.processor.Process;
import com.istat.freedev.processor.ProcessManager;
import com.istat.freedev.processor.interfaces.ProcessListener;
import com.istat.freedev.processor.interfaces.RunnableDispatcher;
import com.istat.freedev.processor.utils.ProcessUnit;

public abstract class AuthenticatedOkHttpProcessUnit extends ProcessUnit {
    public final static String PID_AUTHETICATION_PROCESS = "authentication_pid" + System.currentTimeMillis();

    public AuthenticatedOkHttpProcessUnit() {
        this("AuthenticatedOkHttpProcessUnit:" + System.currentTimeMillis(), null);
    }

    public AuthenticatedOkHttpProcessUnit(String nameSpace) {
        this(nameSpace, null);
    }

    public AuthenticatedOkHttpProcessUnit(String nameSpace, RunnableDispatcher runnableDispatcher) {
        super(nameSpace, runnableDispatcher);
        registerProcessListener(mProcessListener);
    }

    private final ProcessListener mProcessListener = new ProcessListener() {
        @Override
        public void onProcessStarted(Process process, String id) {

        }

        @Override
        public void onProcessStateChanged(final Process process, String id, int state) {
            if (state == AuthenticatedOkHttpProcess.STATE_AUTHENTICATION_FAILURE && process instanceof AuthenticatedOkHttpProcess) {
                //un process a notifier qu'il avait un problème d'autentification. il faut alors lancer un process d'authetification.
                promiseForAuthenticatedState(process);
            }
        }

        @Override
        public void onProcessFinished(Process process, String id) {

        }
    };

    private void promiseForAuthenticatedState(final Process process) {
        OkHttpProcess authenticationProcess;
        try {
            authenticationProcess = proceedAuthentication();
        } catch (Exception e) {
            ((AuthenticatedOkHttpProcess) process).resume(e);
            return;

        }
        authenticationProcess.then(new Process.PromiseCallback() {
            @Override
            public void onPromise(Object data) {
                //l'authetification c'est bien passé.
                process.resume();
            }
        }).abortion(new Process.PromiseCallback<Void>() {
            @Override
            public void onPromise(Void data) {
                process.cancel();
            }
        }).failed(new Process.PromiseCallback<Throwable>() {
            @Override
            public void onPromise(Throwable data) {
                ((AuthenticatedOkHttpProcess) process).resume(data);
            }
        }).error(new Process.PromiseCallback<OkHttpProcess.Error>() {
            @Override
            public void onPromise(OkHttpProcess.Error error) {
                ((AuthenticatedOkHttpProcess) process).resume(error);
            }
        });
    }

    @Override
    public boolean cancel() {
        unRegisterProcessListener(mProcessListener);
        return super.cancel();
    }

    OkHttpProcess proceedAuthentication() throws Exception {
        Process process = getProcessManager().getProcessById(PID_AUTHETICATION_PROCESS);
        if (process instanceof OkHttpProcess) {
            //si un process avec le PID d'authetification est déja en cours, on le retourne simplement.
            return (OkHttpProcess) process;
        }
        OkHttpProcess newAuthenticationProcess = onCreateAuthenticationProcess();
        if (!newAuthenticationProcess.isRunning()) {
            //Si le process retourné n'a pas été demarré, le démarrer automatiquement
            onExecuteAuthenticationProcess(PID_AUTHETICATION_PROCESS, newAuthenticationProcess);
        } else if (!PID_AUTHETICATION_PROCESS.equals(newAuthenticationProcess.getId())) {
            throw new IllegalStateException("The supplyed AuthenticationProcess you have created is already started but with incorrect PID: found=" + newAuthenticationProcess.getId() + "; must be=" + PID_AUTHETICATION_PROCESS);
        }
        return newAuthenticationProcess;
    }

    protected abstract OkHttpProcess onCreateAuthenticationProcess();

    protected void onExecuteAuthenticationProcess(String PID, Process process) throws ProcessManager.ProcessException {
        execute(PID, process);
    }

}
