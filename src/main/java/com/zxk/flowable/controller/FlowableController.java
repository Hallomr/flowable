package com.zxk.flowable.controller;

import com.alibaba.fastjson.JSONObject;
import com.zxk.flowable.vo.req.Leave;
import com.zxk.flowable.vo.req.Candidate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
        //???????????????????????????
        identityService.createMembership(candidate.getUserId(),candidate.getGroupId());
    }

    @GetMapping("/saveGroup/{id}")
    public void saveGroup(@PathVariable("id")String id){
        Group group = identityService.newGroup(id);
        identityService.saveGroup(group);
    }

    /**
     * ??????RuntimeService?????????????????????????????????
     * ?????????????????????java.util.Map?????????????????????????????????????????????????????????????????????
     * ????????????????????????????????????????????????BPMN 2.0 XML ??????????????????id???????????????
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
    public void taskList(@PathVariable("userId") String userId){
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId)
                .orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            log.info("taskName:{},taskId:{}",task.getName(),task.getId());
        }
    }

    /**
     * ???????????????????????????
     * @param groupId
     */
    @GetMapping("/groupTask/{groupId}")
    public void groupTask(@PathVariable("groupId") String groupId){
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(groupId).list();
        for (Task task : tasks) {
            log.info("taskName:{},taskId:{}",task.getName(),task.getId());
            Map<String, Object> variables = taskService.getVariables(task.getId());
            log.info("employee;{},holidays:{},description:{}",
                    variables.get("employee"),variables.get("holidays"),variables.get("description"));
        }
    }

    /**
     * managers??????????????????
     * @param complete
     * @param taskId
     */
    @GetMapping("/completeTask/{complete}/{taskId}")
    public void completeTask(@PathVariable("complete") boolean complete,@PathVariable("taskId") String taskId){
        Map<String,Object> variables = new HashMap<>();
        variables.put("approved", complete);
        taskService.complete(taskId, variables);
    }

    /**
     * ???????????????
     * @param response
     * @param processId
     * @throws IOException
     */
    @GetMapping("/processDiagram/{processId}")
    public void getProcessDiagram(HttpServletResponse response,@PathVariable("processId") String processId) throws IOException {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        //???????????????????????????
        if(pi==null){
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        String instanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(instanceId).list();

        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for(Execution exe:executions){
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }
        //???????????????
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator processDiagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = processDiagramGenerator.generateDiagram(bpmnModel,"png",activityIds,flows,
                engconf.getActivityFontName(),engconf.getLabelFontName(),engconf.getAnnotationFontName(),
                engconf.getClassLoader(),1.0,true);
        OutputStream out = null;
        try{
            out = response.getOutputStream();
            IOUtils.write(IOUtils.toByteArray(in),out);
        }finally{
            if(in!=null){
                in.close();
            }
            if(out!=null){
                out.close();
            }
        }
    }


    /**
     * ???????????? Flowable ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????
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
