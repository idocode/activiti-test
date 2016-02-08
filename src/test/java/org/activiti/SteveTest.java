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
public class SteveTest {

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
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("steveProcessWithJpa", variables);

        // First, the 'phone interview' should be active
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("dev-managers")
                .singleResult();
        Assert.assertEquals("Telephone interview", task.getName());

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        taskService.complete(task.getId(), taskVariables);

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .orderByTaskName().asc()
                .list();
        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals("Financial negotiation", tasks.get(0).getName());
        Assert.assertEquals("Tech interview", tasks.get(1).getName());

        // Completing both should wrap up the subprocess, send out the 'welcome mail' and end the process instance
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("financeOk", true);
        taskVariables.put("errorCase", false);
        taskService.complete(tasks.get(0).getId(), taskVariables);

        taskVariables = new HashMap<String, Object>();
        taskVariables.put("techOk", true);
        taskService.complete(tasks.get(1).getId(), taskVariables);

        
        // Finally, the 'Final Task' should be active
        Task finaltask = taskService.createTaskQuery()        		        	
                .singleResult();
        Assert.assertEquals("Final Task", finaltask.getName());

        // Completing the final task will complete the workflow
        taskService.complete(finaltask.getId());
        
        
        // Verify process completed
        Assert.assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());

    }
    @Test
    @Ignore
    public void testErrorCondition() {

        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@activiti.org", "12344");
        applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("steveProcessWithJpa", variables);

        // First, the 'phone interview' should be active
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("dev-managers")
                .singleResult();
        Assert.assertEquals("Telephone interview", task.getName());

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        taskService.complete(task.getId(), taskVariables);

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .orderByTaskName().asc()
                .list();
        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals("Financial negotiation", tasks.get(0).getName());
        Assert.assertEquals("Tech interview", tasks.get(1).getName());

        // Complete the tech task first
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("techOk", true);
        taskService.complete(tasks.get(1).getId(), taskVariables);
        
        // Now the financial task with error which should end the process
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("financialOk", true);
        taskVariables.put("errorCase", true);
        taskService.complete(tasks.get(0).getId(), taskVariables);


/*        
        // Finally, the 'Final Task' should be active
        Task finaltask = taskService.createTaskQuery()        		        	
                .singleResult();
        Assert.assertEquals("Final Task", finaltask.getName());

        // Completing the final task will complete the workflow
        taskService.complete(finaltask.getId());
*/        
       
        // Verify process completed
        Assert.assertEquals(2, historyService.createHistoricProcessInstanceQuery().finished().count());

    }

}
