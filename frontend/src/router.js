import { createRouter, createWebHashHistory } from "vue-router";
import { BASE_URL } from './main.js'

// 懒加载组件
const Home = () => import("./components/Home.vue");
const Login = () => import("./components/Login.vue");
const Chat = () => import("./components/Chat.vue");

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: "/",
            name: "home",
            component: Home,
            meta: {
                title: "儿童故事管理平台"
            }
        },
        {
            path: "/login",
            name: "login",
            component: Login,
            meta: {
                title: "登录 - 儿童故事管理平台"
            }
        },
        {
            path: "/chat",
            name: "chat",
            component: Chat,
            meta: {
                title: "故事对话 - 儿童故事管理平台",
                requiresAuth: true
            }
        }
    ]
})

// 全局前置守卫，设置页面标题和登录检查
router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title || "儿童故事管理平台";
    
    // 检查路由是否需要认证
    if (to.matched.some(record => record.meta.requiresAuth)) {
        try {
            // 检查Session是否有效
            const response = await fetch(`${BASE_URL}/api/users/checkSession`, {
                credentials: 'include' // 包含cookie以维护Session
            });
            
            if (response.ok) {
                // Session有效，继续访问
                next();
            } else {
                // 未登录或Session过期，重定向到登录页面
                next({
                    path: '/login',
                    query: { redirect: to.fullPath }
                });
            }
        } catch (error) {
            console.error('检查登录状态失败:', error);
            // 出错时重定向到登录页面
            next({
                path: '/login',
                query: { redirect: to.fullPath }
            });
        }
    } else {
        next();
    }
});

export default router;