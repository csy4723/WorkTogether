package com.uni.wt.employee.controller;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.uni.wt.employee.model.dto.Employee;
import com.uni.wt.employee.model.service.EmployeeService;

/**
 * Handles requests for the application home page.
 */
@SessionAttributes("loginEmp")
@Controller
public class EmployeeController {
	
	private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
	
	@Autowired
	private BCryptPasswordEncoder bPwdEncoder;
	
	@Autowired
	private EmployeeService empService;
	
	private Map<String, String> msgMap = new HashMap<String, String>(); 
	
	
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		log.info("Welcome home! The client locale is {}.", locale);
		
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
		
		String formattedDate = dateFormat.format(date);
		
		model.addAttribute("serverTime", formattedDate );
		
		return "common/main";

	}



	@RequestMapping("/allProject")
	public String allProject() {
		return "project/allProject";
	}
	
	@RequestMapping("/detailPj.do")
	public String detailPj() {
		return "project/detailPj";
	}
	
	
	@RequestMapping(value="/enrollForm.do")
	public String enrollForm() {
		
		return "employee/register";
		
	}
	
	@RequestMapping(value="/loginForm.do")
	public String loginForm(HttpServletRequest request, Model m) {
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		
		if(flashMap != null) {
			Map<String, String> params = (Map<String, String>) flashMap.get("msg");
			
			String msg = params.get("msg");
			
			m.addAttribute("msg", msg);
		}
		return "employee/login";
	}
	
	@RequestMapping("/login.do")
	public String login(HttpServletRequest request, Model m, Employee emp, RedirectAttributes redirect) throws Exception {
		
		log.info("ID : {}",emp.getId());
		log.info("password : {}", emp.getPassword());
		
		Employee loginEmp = empService.loginEmp(emp);
		if(loginEmp == null) {//Emp를 가져오지 못했으면 msg와 로그인 폼으로 돌아감
			msgMap.put("msg", "아이디가 일치하지 않습니다");
			redirect.addFlashAttribute("msg", msgMap);
			//redirect할 때, model에 직접 msg를 담으면 get 형식으로 가고, Map에 담아 model로 보내면 보내지지 않는다.
			//addAttribute로 전달한 값은 url뒤에  붙으며, 리프레시해도 데이터가 유지된다.
			//addFlashAttribute로 전달한 값은 url뒤에 붙지 않는다. 일회성이라 리프레시할 경우 데이터가 소멸한다.
			//또한 2개이상 쓸 경우, 데이터는 소멸한다. 따라서 맵을 이용하여 한번에 값전달해야한다.
			return "redirect:/loginForm.do";
			
		}
		String login = loginEmp.toString();
		log.info(login); // 담아온 객체 확인
		
		if(!bPwdEncoder.matches(emp.getPassword(), loginEmp.getPassword())) {
			//bPwdEncoder.encode한 암호는 매번 다르기 때문에 matches로 비교해줘야 일치하는지 알 수 있다.
			msgMap.put("msg", "비밀번호가 일치하지 않습니다.");
			redirect.addFlashAttribute("msg", msgMap);
			return "redirect:/loginForm.do";
		}
		
		String st = loginEmp.getStatus();
		
		if(st.equals("W")) {
			 msgMap.put("msg", "가입 승인 대기 중입니다.");
			 redirect.addFlashAttribute("msg", msgMap);
			 return "redirect:/loginForm.do";
		}else if(st.equals("R")){
			msgMap.put("msg", "가입승인이 반려되었습니다.");
			redirect.addFlashAttribute("msg", msgMap);
			return "redirect:/loginForm.do";
		}
		
		m.addAttribute("loginEmp", loginEmp);// Redirect로 전달되지 않지만 @SessionAttributes로 세션에 자동 저장
		
		return "redirect:/";
	}
	
	@RequestMapping("/loout.do")
	public String logout(SessionStatus status) {
		status.setComplete();
		
		return "redirect:/loginForm.do";
		
	}
	
	
	//
	
	@RequestMapping(value="/enrollEmp.do")
	public String enrollEmp (@Valid Employee emp, Model m, RedirectAttributes redirect) throws Exception {
		
		log.info(emp.toString());
		log.info("암호화 전 : "+emp.getPassword());
		String encPwd = bPwdEncoder.encode(emp.getPassword());
		log.info("암호화 후 : "+encPwd);
		
		emp.setPassword(encPwd);
		
		
		empService.insertEmp(emp);
		
		msgMap.put("msg", "회원가입에 성공했습니다.");
		redirect.addFlashAttribute("msg", msgMap);
		
		
		return "redirect:/loginForm.do";
	}
	
	@ResponseBody//데이터 그대로 가져가야 한다. 
	@RequestMapping("/idCheck.do")//숫자를 가져가기 때문에 굳이 인코딩해줄 필요가 없다. 
	public String idCheck(String userId) throws Exception {
		
		log.info("중복 id 체크");
		int count = empService.idCheck(userId);//중복된 아이디의 개수를 가져온다. 
		log.info("중복된 아이디 개수 : "+count);
		return String.valueOf(count);//파싱문제가 있기때문에 String으로 반환한다. 
	}
		


	
	
}
