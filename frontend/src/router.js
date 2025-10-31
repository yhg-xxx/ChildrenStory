import { createRouter, createWebHashHistory } from "vue-router";

// 懒加载 StoryGenerator 组件
const StoryGenerator = () => import("./components/StoryGenerator.vue");

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: "/",
            name: "home",
            component: StoryGenerator,
            meta: {
                title: "AI故事生成器"
            }
        },
        {
            path: "/generate",
            name: "generate",
            component: StoryGenerator,
            meta: {
                title: "生成故事"
            }
        }
    ]
})
// 全局前置守卫，设置页面标题
router.beforeEach((to, from, next) => {
    document.title = to.meta.title || "AI故事生成平台";
    next();
});

export default router;