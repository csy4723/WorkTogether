package com.uni.wt.requestWork.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.google.gson.GsonBuilder;
import com.uni.wt.common.Pagination;
import com.uni.wt.common.commonFile.FileService;
import com.uni.wt.common.dto.PageInfo;
import com.uni.wt.common.notice.service.NoticeService;
import com.uni.wt.employee.model.dto.Employee;
import com.uni.wt.project.model.dto.Project;
import com.uni.wt.requestWork.model.dto.RequestWork;
import com.uni.wt.requestWork.model.service.RequestService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/requestWork")
public class RequestWorkController {
	
	@Autowired
	private RequestService rwService;
	
	@Autowired
	private FileService fileService;
	
//	@Autowired
//	private EchoHandler echoHandler;
	
	@Autowired
	private NoticeService noticeService;
	
	
	@RequestMapping("/requestWorkMain.do")
	public String requestWorkMain(HttpServletRequest request, Model m) throws Exception {
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		
		if(flashMap != null) {
			Map<String, String> params = (Map<String, String>) flashMap.get("msg");
			
			String msg = params.get("msg");
			
			m.addAttribute("msg", msg);
		}
		
		Employee emp = (Employee)request.getSession().getAttribute("loginEmp");
		log.info(emp.toString());
		ArrayList<Object> deptList = rwService.selectDeptList();
		m.addAttribute("deptList", deptList);
		ArrayList<Project> prjList = rwService.selectProjectList(emp.getEmp_no());
		log.info("참여중 project 목록 : {}", prjList);
		m.addAttribute("prjList", prjList);
		
		///////메인화면 데이터 
		
		// 새 업무요청과 내가 진행중인 업무요청
		Map<String, Object> cnt = rwService.selectCountRw(emp.getEmp_no());
		if(cnt != null) {
			
			m.addAttribute("RQ", cnt.get("RQ"));
			m.addAttribute("P", cnt.get("P"));
		}
		
		///요청한 업무 목록 
		Map<String, Object> reqmap = requestList(emp.getEmp_no(), 1);
		//요청받은 업무 목록 
		Map<String, Object> resmap = responsList(emp.getEmp_no(), 1);
		
		//요청한
		m.addAttribute("reqList", reqmap.get("list"));
		m.addAttribute("reqpi", reqmap.get("pi"));
		
		//요청된
		m.addAttribute("resList", resmap.get("list"));
		m.addAttribute("respi", resmap.get("pi"));
		
		
		
		
		
		
		return "requestwork/rwMain";
	}
	
	//메인 요청된 업무 리스트
	private Map<String, Object> responsList(int emp_no, int currentPage) throws Exception {
		//요청된 업무 리스트 전체 개수
		int listCount = rwService.getListCount(emp_no, "RS");
		log.info("[요청받은업무 리스트 개수] : {}", listCount);
		
		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, 5, 10);
		
		ArrayList<RequestWork> list = rwService.selectRSList(emp_no, pi);
		log.info("[요청받은업무 리스트] : {}", list);
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("list", list);
		map.put("pi", pi);

		return map;
	}
	//메인 요청한 업무 리스트 
	private Map<String, Object> requestList(int emp_no, int currentPage) throws Exception {
		//업무요청한 리스트 전체 개수
		int listCount = rwService.getListCount(emp_no, "RQ");
		log.info("[업무요청한 리스트 개수] : {}", listCount);
		
		PageInfo pi = Pagination.getPageInfo(listCount, currentPage, 5, 10);
		
		ArrayList<RequestWork> list = rwService.selectRQList(emp_no, pi);
		log.info("[요청한 업무 리스트] : {}", list);
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("list", list);
		map.put("pi", pi);
		
		return map;
	}

	@ResponseBody
	@RequestMapping(value = "/getDeptMember.do", produces = "application/text; charset=UTF-8")
	public String getDeptMember(String dept_code)throws Exception{
		log.info("부서 번호 : {}", dept_code);
		
		ArrayList<Employee> memberList = rwService.getDeptMember(Integer.parseInt(dept_code));
		
		log.info(dept_code+"부서의 인원 : "+memberList.toString());
		
		return new GsonBuilder().setDateFormat("yyyy-MM-dd").create().toJson(memberList);
	}
	
	@RequestMapping("/insertRequestWork.do")
	public String insertRequestWork(RequestWork rw, @RequestParam(name="upload_file", required=false) MultipartFile file,
			HttpServletRequest request, RedirectAttributes redirect) throws Exception {
		Map<String, String> msgMap = new HashMap<String, String>(); 
		Employee emp = (Employee)request.getSession().getAttribute("loginEmp");
		log.info("[로그인된 유저] : {}", emp.toString());
		
		if(rw.getPj_no().equals("0")) {//선택한 프로젝트 번호가 0번이면 
			rw.setPj_no(null);//프로젝트 없음
		}
		
		String fileSeq = null;//null이 넣어지니까 
		if(!file.getOriginalFilename().equals("")) {//만약 받아온 파일이 비어 있어있지 않으면
			fileSeq = String.valueOf(fileService.uploadFile(file, request, "RW"));
			
			rw.setReq_file(fileSeq);
			
		}
		
		rw.setReq_member(String.valueOf(emp.getEmp_no()));
		
		int result = rwService.insertRequestWork(rw);
		log.info("업무신청서 결과 : {}", result);
		
		if(result > 0) {
			msgMap.put("msg", "업무가 성공적으로 요청됐습니다.");
			
			noticeService.insertNotice(emp, result, "RW", request);// 알림 insert
			
			
		}else {
			fileService.failed(Integer.parseInt(fileSeq));
			msgMap.put("msg", "업무 요청에 실패했습니다.");
			
		}
		redirect.addFlashAttribute("msg", msgMap);
		
		
		
		
		return "redirect:/requestWork/requestWorkMain.do";
	}
	

	@RequestMapping("/completedRequest.do")
	public String completedRequest() {
		
		return "requestwork/rwCompleted";
	}
	
	@RequestMapping("/allRequest.do")
	public String allRequest() {
		
		return "requestwork/rwAllList";
	}

}
