package cn.e3mall.cart.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.e3mall.cart.service.CartService;
import cn.e3mall.common.utils.CookieUtils;
import cn.e3mall.common.utils.E3Result;
import cn.e3mall.common.utils.JsonUtils;
import cn.e3mall.pojo.TbItem;
import cn.e3mall.pojo.TbUser;
import cn.e3mall.service.ItemService;

/**  

* <p>Title: CartController</p>  

* <p>Description:购物车controller</p>  

* @author 赵天宇

* @date 2019年1月25日  

*/
@Controller
public class CartController {
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private ItemService itemService;
	
	@Value("${COOKIE_CART_EXPIRE}")
	private Integer COOKIE_CART_EXPIRE;
	
	/**
	 * 
	
	 * <p>Title: addCart</p>  
	
	 * <p>Description:添加到购物车（用户登录情况下，存入Redis，未登录情况下存入cookie） </p>  
	
	 * @param itemid
	 * @param num
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/cart/add/{itemid}")
	public String addCart(@PathVariable Long itemid,@RequestParam(defaultValue="1")Integer num
			,HttpServletRequest request,HttpServletResponse response){
//判断用户是否登录
TbUser user = (TbUser) request.getAttribute("user");
//如果登录，把过购物车写入redis
if(user != null){
//保存到服务端
	cartService.addcart(user.getId(), itemid, num);
//返回逻辑视图
return "cartSuccess";
			
}
//如果没有登录，把购物车写入cookie
		//1.从cookie中取商品列表
		List<TbItem> list = getcartfromcookie(request);
		boolean flag=false;//标识（用来给第三步判断，商品到底在列表中存不存在）
		//2.判断商品是否在商品列表里面存在
		for (TbItem tbItem : list) {
				//对象比较的是地址，应该是值的比较（只要商品id相同，就是存在)
				if(tbItem.getId() == itemid.longValue()){
					flag=true;
					//如果存在，则数量相加
					tbItem.setNum(tbItem.getNum()+num);
					//找到后跳出循环
					break;
				}
		}
		//3.如果不存在，则根据itemid查询商品信息，得到一个Tbitem的对象
		if(!flag){
		TbItem tbItem = itemService.getItemById(itemid);
			//取一张图片（购物车里面只需要显示一张图片即可）
			String image = tbItem.getImage();
			if(StringUtils.isNotBlank(image)){
				String[] images = image.split(",");
				tbItem.setImage(images[0]);
			}
			//设置购买商品数量
			tbItem.setNum(num);
			//4.把商品添加到商品列表
			list.add(tbItem);
		}
		//5.写入cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(list), COOKIE_CART_EXPIRE, true);
		//6.返回添加成功页面
		return "cartSuccess";
	}
	
	
	/**  
	
	 * <p>Title: getcartfromcookie</p>  
	
	 * <p>Description: 从cookie中取商品列表的处理</p>  
	
	 * @param request
	 * @return  
	
	 */  
	private List<TbItem> getcartfromcookie(HttpServletRequest request){
		String json = CookieUtils.getCookieValue(request, "cart", true);
		//判断json是否为空
		if(StringUtils.isBlank(json)){
			return new ArrayList<>();
		}
		//把json转换为商品列表List<TbItem>
		List<TbItem> list = JsonUtils.jsonToList(json, TbItem.class);
		return list;
	}
	
	/**
	 * 
	
	 * <p>Title: showcart</p>  
	
	 * <p>Description: 在购物车结算页面取出cookie中的商品列表（同样有两种登录、未登录情况）</p>  
	
	 * @return
	 */
	@RequestMapping("/cart/cart")
	public String showcart(HttpServletRequest request,HttpServletResponse response){
		//从cookie中取商品列表
		List<TbItem> cartlist = getcartfromcookie(request);
		
		//判断是否登录
		TbUser user = (TbUser) request.getAttribute("user");
		//如果登录，从cookie中取商品列表
		if(user!=null){
		//如果cookie中取出商品列表不是空，和服务端的商品列表合并
			cartService.mergecart(user.getId(), cartlist);
		//然后把cookie中的商品列表删除
			CookieUtils.deleteCookie(request, response, "cart");
		//从服务端获取商品列表
			cartlist= cartService.getCart(user.getId());
		}
			//未登录的情况下
			//把列表传递给页面
			request.setAttribute("cartlist", cartlist);
			//返回逻辑视图
			return "cart";
	}
	
	/**
	 * 
	
	 * <p>Title: updatecartnum</p>  
	
	 * <p>Description: 更新购物车的数量</p>  
	
	 * @param itemId
	 * @param num
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/cart/update/num/{itemId}/{num}")
	@ResponseBody
	public E3Result updatecartnum(@PathVariable Long itemId,@PathVariable Integer num,
			HttpServletRequest request,HttpServletResponse response){
		//判断是否登录
		TbUser user = (TbUser) request.getAttribute("user");
		//如果登录了
		if(user!=null){
			cartService.updatecartNum(user.getId(), itemId, num);
			E3Result.ok();
		}
		
		//未登录的情况下
		//从cookie中取商品列表
		List<TbItem> cartlist = getcartfromcookie(request);
				//遍历列表取找到对应的商品
		for (TbItem tbItem : cartlist) {
			if(tbItem.getId().longValue()==itemId){
				//更新数量
				tbItem.setNum(num);
				break;
			}
		}
				//吧列表写入cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(cartlist), COOKIE_CART_EXPIRE, true);
				//返回成功
		return E3Result.ok();
		}
	
	/**
	 * 
	
	 * <p>Title: deletecart</p>  
	
	 * <p>Description:购物车里面进行删除 </p>  
	
	 * @param itemId
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/cart/delete/{itemId}")
	public String deletecart(@PathVariable Long itemId,HttpServletRequest request,
			HttpServletResponse response){
		//判断是否登录
		TbUser user = (TbUser) request.getAttribute("user");
		//如果登录了
		if(user!=null){
			cartService.delcart(user.getId(), itemId);
			return "redirect:/cart/cart.html";
				}
		
		
		//未登录的情况下
			//从cookie中取商品列表
		List<TbItem> cartlist = getcartfromcookie(request);
			//便利列表，找到要删除的商品
		for (TbItem tbItem : cartlist) {
			if(tbItem.getId().longValue() == itemId){
				cartlist.remove(tbItem);
				break;
			}
		}
			//吧购物车写入cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(cartlist), COOKIE_CART_EXPIRE, true);
			//返回逻辑视图（重定向）
		return "redirect:/cart/cart.html";
	}

}
