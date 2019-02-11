package cn.e3mall.cart.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.e3mall.common.utils.CookieUtils;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.pojo.TbUser;
import cn.e3mall.sso.service.TokenService;

/**  

* <p>Title: Logininterceptor</p>  

* <p>Description: 拦截器用户登录处理</p>  

* @author 赵天宇

* @date 2019年1月28日  

*/
public class Logininterceptor implements HandlerInterceptor {
	
	
	@Autowired
	private TokenService tokenService;
	
	/**
	 * preHandle  前处理，执行handler之前执行此方法
	 * 返回true  放行，   false ：拦截
	 * 
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		// 1.从cookie中取出token
		String token = CookieUtils.getCookieValue(request, "token");
		// 2.如果没有token，则就是未登录状态，直接放行
		if(StringUtils.isBlank(token)){
			return true;
		}
		// 3.如果有token，需要调用sso系统的服务，根据token取出对应的用户信息
		E3Result e3Result = tokenService.getuserbytoken(token);
		// 4.如果没有取到用户信息，则是登录过期，放行
		if(e3Result.getStatus()!= 200){
			return true;
		}
		// 5。取到用户信息，则是属于登录状态
		TbUser user = (TbUser) e3Result.getData();
		// 6.把用户信息放到request中，只需要在controller中判断request中是否包含user用户信息，放行
		request.setAttribute("user", user);
		return true;
	}
	
	/**
	 * postHandle  执行handler方法之后，返回ModelAndView之前
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * afterCompletion  已经完成了处理，返回ModelAndView之后
	 * 可以在此 进行异常的处理
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e)
			throws Exception {
		// TODO Auto-generated method stub

	}

	

	

}
