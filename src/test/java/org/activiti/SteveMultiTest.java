package org.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MyApp.class})
@WebAppConfiguration
@IntegrationTest
public class SteveMultiTest {

    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private HistoryService historyService;
    @Autowired private ApplicantRepository applicantRepository;


    @Before
    public void setup() {
    }

    @After
    public void cleanup() {
    }

    @Test
    @Ignore
    public void testHappyPath() {

        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@activiti.org", "12344");
        applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("steveMultiWithJpa", variables);

        // First, the photo task should be active
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("photographers")
                .singleResult();
        Assert.assertEquals("Photo Task", task.getName());

        // Completing the photo task with hasImage == true creates a ready to edit task
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("hasImage", true);
        taskService.complete(task.getId(), taskVariables);

        // Find the ready to edit task
        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("digitech")
                .singleResult();
        Assert.assertEquals("Ready Edit", task.getName());

        // complete the ready to edit task - should be at join
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("errorCase", false);
        taskService.complete(task.getId(), taskVariables);

        
        // Find the second photo task
        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("photographers")
                .singleResult();
        Assert.assertEquals("Photo Task", task.getName());
        
        // Completing the photo task with hasImage == false creates a ship sample task
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("hasImage", false);
        taskService.complete(task.getId(), taskVariables);

        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("shippers")
                .singleResult();
        Assert.assertEquals("Ship Sample", task.getName());

        
        // store number of images in process variable?
        // exclusive gateway if number of images == 0 after ship sample then complete?
        
        // complete the ship task - should be at join
        taskService.complete(task.getId());
        
        // Verify process completed
        Assert.assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());

    }
    @Test
    @Ignore
    public void testMultipleImages() {

        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@activiti.org", "12344");
        applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("steveMultiWithJpa", variables);

        // First, the photo task should be active
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("photographers")
                .singleResult();
        Assert.assertEquals("Photo Task", task.getName());

        // Completing the photo task with hasImage == true creates a ready to edit task
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("hasImage", true);
        taskService.complete(task.getId(), taskVariables);

        // Find the ready to edit task
        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("digitech")
                .singleResult();
        Assert.assertEquals("Ready Edit", task.getName());

        // Find the new photo task
        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("photographers")
                .singleResult();
        Assert.assertEquals("Photo Task", task.getName());

        // Completing the photo task with hasImage == true creates a ready to edit task
        // second ready to edit
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("hasImage", true);
        taskService.complete(task.getId(), taskVariables);

        // should be 2 ready to edit tasks now
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("digitech")
                .list();
        Assert.assertEquals(2, tasks.size());
        
        
        // complete the ready to edit task - should be at join
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("errorCase", false);
        taskService.complete(tasks.get(0).getId(), taskVariables);
        taskService.complete(tasks.get(1).getId(), taskVariables);

        
        // Find the second photo task
        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("photographers")
                .singleResult();
        Assert.assertEquals("Photo Task", task.getName());
        
        // Completing the photo task with hasImage == false creates a ship sample task
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("hasImage", false);
        taskService.complete(task.getId(), taskVariables);

        task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("shippers")
                .singleResult();
        Assert.assertEquals("Ship Sample", task.getName());

        
        // store number of images in process variable?
        // exclusive gateway if number of images == 0 after ship sample then complete?
        
        // complete the ship task - should be at join
        taskService.complete(task.getId());
        
        tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .orderByTaskName().asc()
                .list();
 
        Assert.assertEquals(0, tasks.size());
        
        // Verify process completed
        Assert.assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());


    }

}
