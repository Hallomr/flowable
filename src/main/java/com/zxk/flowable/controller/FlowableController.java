package com.zxk.flowable.controller;

import com.alibaba.fastjson.JSONObject;
import com.zxk.flowable.vo.req.Leave;
import com.zxk.flowable.vo.req.Candidate;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flowable")
@Slf4j
public class FlowableController {
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private TaskService taskService;


    @GetMapping("/deploy")
    public String deploy(){
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("Holiday_Request.bpmn20.xml").deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploy.getId()).singleResult();
        return processDefinition.getName();
    }

    @PostMapping("/saveCandidate")
    public void saveCandidate(@RequestBody Candidate candidate){
        User user = identityService.newUser(candidate.getUserId());
        user.setFirstName(candidate.getFirstName());
        user.setLastName(candidate.getLastName());
        user.setEmail(candidate.getEmail());
        user.setPassword(candidate.getPassword());
        identityService.saveUser(user);
        //添加用户和组的关联
        identityService.createMembership(candidate.getUserId(),candidate.getGroupId());
    }

    @GetMapping("/saveGroup/{id}")
    public void saveGroup(@PathVariable("id")String id){
        Group group = identityService.newGroup(id);
        identityService.saveGroup(group);
    }

    /**
     * 通过RuntimeService启动一个请假流程实例。
     * 收集的数据作为java.util.Map实例传递，其中键是稍后将用于检索变量的标识符。
     * 流程实例是使用密钥启动的。此键与BPMN 2.0 XML 文件中设置的id属性相匹配
     * @param leave
     */
    @PostMapping("/leave")
    public void leave(@RequestBody Leave leave){
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map map = JSONObject.parseObject(JSONObject.toJSONString(leave), Map.class);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", map);
        log.info("processInstanceId:{}",processInstance.getId());
    }

    @GetMapping("/taskList/{userId}")
    public List<Task> taskList(@PathVariable("userId") String userId){
        return taskService.createTaskQuery().taskAssignee(userId)
                .orderByTaskCreateTime().desc().list();
    }

    @GetMapping("/completeTask/{complete}/{taskId}")
    public void completeTask(@PathVariable("complete") boolean complete,@PathVariable("taskId") String taskId){
        Map<String,Object> variables = new HashMap<>();
        variables.put("approved", complete);
        taskService.complete(taskId, variables);
    }

    /**
     * 选择使用 Flowable 这样的流程引擎的众多原因之一是因为它会自动存储所有流程实例的审计数据或历史数据。
     * 这些数据允许创建丰富的报告，深入了解组织的运作方式、瓶颈所在等
     */
    @GetMapping("/analysis/{id}")
    public void historyAnalysis(@PathVariable("id") String processInstanceId){
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }
    }
}
