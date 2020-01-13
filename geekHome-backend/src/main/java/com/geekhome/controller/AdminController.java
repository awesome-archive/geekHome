package com.geekhome.controller;

import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.geekhome.common.vo.ErrorCode;
import com.geekhome.common.vo.ExecuteResult;
import com.geekhome.common.vo.RetJson;
import com.geekhome.entity.Admin;
import com.geekhome.entity.AdminRole;
import com.geekhome.entity.Role;
import com.geekhome.entity.dao.AdminDao;
import com.geekhome.entity.dao.AdminRoleDao;
import com.geekhome.entity.dao.RoleDao;
import com.geekhome.entity.service.AdminService;

@RestController
@RequestMapping("admin")
public class AdminController {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	AdminDao adminDao;
	@Autowired
	AdminService adminService;
	@Autowired
	RoleDao roleDao;
	@Autowired
	AdminRoleDao adminRoleDao;

	@RequestMapping("loginPage")
	public ModelAndView loginPage() {
		return new ModelAndView("login");
	}

	@RequestMapping(value = "login", method = RequestMethod.POST)
	public ModelAndView loginPost(String username, String password) {

		ModelAndView view = new ModelAndView();
		UsernamePasswordToken token = new UsernamePasswordToken(username, password);
		// 获取当前的Subject
		Subject currentUser = SecurityUtils.getSubject();
		try {
			logger.info("对用户[" + username + "]进行登录验证..验证开始");
			currentUser.login(token);
			logger.info("对用户[" + username + "]进行登录验证..验证通过");
		} catch (UnknownAccountException uae) {
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,未知账户");
			view.addObject("message", "未知账户");
		} catch (IncorrectCredentialsException ice) {
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误的凭证");
			view.addObject("message", "密码不正确");
		} catch (LockedAccountException lae) {
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,账户已锁定");
			view.addObject("message", "账户已锁定");
		} catch (ExcessiveAttemptsException eae) {
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误次数过多");
			view.addObject("message", "用户名或密码错误次数过多");
		} catch (AuthenticationException ae) {
			// 通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
			logger.info("对用户[" + username + "]进行登录验证..验证未通过,堆栈轨迹如下");
			ae.printStackTrace();
			view.addObject("message", "用户名或密码不正确");
		}
		// 验证是否登录成功
		if (currentUser.isAuthenticated()) {
			Session session = SecurityUtils.getSubject().getSession();
			Admin admin = (Admin) currentUser.getPrincipal();
			session.setAttribute("admin", admin);
			logger.info("用户[" + username + "]登录认证通过");
			view.setViewName("index");
		} else {
			token.clear();
			view.setViewName("login");
		}
		return view;
	}

	@RequestMapping("logout")
	public ModelAndView logout() {
		SecurityUtils.getSubject().logout();
		return new ModelAndView("login");
	}
	
	@RequiresPermissions("admin:index")
	@RequestMapping("adminPage")
	public ModelAndView adminPage() {
		return new ModelAndView("/view/system/adminPage");
	}
	
	@RequiresPermissions("admin:list")
	@RequestMapping(value = "/adminList")
	public RetJson adminList(Integer draw, Integer length, Integer start) {
		RetJson retJson = new RetJson();
		try {
			retJson.setStatus(1);
			retJson.setMessage("加载成功");
			Page<Admin> list = adminService.getAdminList(start,length);
			retJson.setData(list.getContent());
			retJson.setRecordsTotal(list.getTotalElements());
			retJson.setRecordsFiltered(list.getTotalElements());
			retJson.setDraw(draw == null ? 0 : draw);
		} catch (Exception e) {
			logger.error("", e);
			retJson.setStatus(-1);
			retJson.setMessage("程序出错");
		}

		return retJson;
	}
	
	@RequestMapping("roleList")
	public ExecuteResult<List<Role>> roleList() {
		final ExecuteResult<List<Role>> result = new ExecuteResult<>();
		try {
			List<Role> list = roleDao.findRoleByStateNot(Role.ROLE_STATE_CLOSE);
			result.setData(list);
			result.setSuccess(true);
		} catch (final Exception e) {
			logger.error("", e);
			result.setSuccess(false);
			result.setErrorCode(ErrorCode.EXCEPTION.getErrorCode());
			result.setErrorMsg(ErrorCode.EXCEPTION.getErrorMsg());
		}
		return result;
	}
	
	@RequestMapping("adminRoleList")
	public ExecuteResult<List<AdminRole>> adminRoleList(Long id) {
		final ExecuteResult<List<AdminRole>> result = new ExecuteResult<>();
		try {
			List<AdminRole> list = adminRoleDao.findAdminRoleByAdminId(id);
			result.setData(list);
			result.setSuccess(true);
		} catch (final Exception e) {
			logger.error("", e);
			result.setSuccess(false);
			result.setErrorCode(ErrorCode.EXCEPTION.getErrorCode());
			result.setErrorMsg(ErrorCode.EXCEPTION.getErrorMsg());
		}
		return result;
	}
	
	
	@RequiresPermissions("admin:save")
	@RequestMapping("saveAdmin")
	public ExecuteResult<Integer> saveAdmin(@RequestBody Admin admin) {
		final ExecuteResult<Integer> result = new ExecuteResult<>();
		try {
			Integer flag = adminService.saveAdmin(admin);
			result.setData(flag);
			result.setSuccess(true);
		} catch (final Exception e) {
			logger.error("", e);
			result.setSuccess(false);
			result.setErrorCode(ErrorCode.EXCEPTION.getErrorCode());
			result.setErrorMsg(ErrorCode.EXCEPTION.getErrorMsg());
		}
		return result;
	}
	
	@RequiresPermissions("admin:delete")
	@RequestMapping("delAdmin")
	public ExecuteResult<Boolean> delAdmin(Long id) {
		final ExecuteResult<Boolean> result = new ExecuteResult<>();
		try {
			adminDao.delete(id);
			adminRoleDao.deleteAdmin(id);
			result.setSuccess(true);
		} catch (final Exception e) {
			logger.error("", e);
			result.setSuccess(false);
			result.setErrorCode(ErrorCode.EXCEPTION.getErrorCode());
			result.setErrorMsg(ErrorCode.EXCEPTION.getErrorMsg());
		}
		return result;
	}
}
