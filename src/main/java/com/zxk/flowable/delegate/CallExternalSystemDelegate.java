package com.zxk.flowable.delegate;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
@Slf4j
public class CallExternalSystemDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Calling the external system for employee "
                + delegateExecution.getVariable("employee"));
    }
}
