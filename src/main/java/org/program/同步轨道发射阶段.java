package org.program;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;
import java.util.List;


import static krpc.client.services.SpaceCenter.SASMode.STABILITY_ASSIST;

public class 同步轨道发射阶段 {
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        Connection 游戏连接 = Connection.newInstance("同步轨道卫星发射程序");
        SpaceCenter 太空中心 = SpaceCenter.newInstance(游戏连接);
        SpaceCenter.Vessel 星舰 = 太空中心.getActiveVessel();
        SpaceCenter.Vessel 着陆器;


// 设置遥测流
        // 用于获取当前的通用时间
        太空中心.getUT();
        //持续监听SpaceCenter对象的getUT方法返回值的变化
        Stream<Double> 世界时间 = 游戏连接.addStream(SpaceCenter.class, "getUT");
        //当前飞船所在星球表面的 参考系
        SpaceCenter.ReferenceFrame 参考系 = 星舰.getSurfaceReferenceFrame();
        //获取了当前飞船的 飞行状态，包括位置、速度等信息
        SpaceCenter.Flight 飞行状态 = 星舰.flight(参考系);
        //持续监听飞船的 平均高度 的变化。getMeanAltitude方法返回飞船在其轨道上的平均高度。
        Stream<Double> 平均高度 = 游戏连接.addStream(飞行状态, "getMeanAltitude");
        //监听飞船轨道上 远地点 的高度变化
        Stream<Double> 远地点 =
                游戏连接.addStream(星舰.getOrbit(), "getApoapsisAltitude");

//=======================发射阶段============================
        {
            // 启动前设置
            星舰.getControl().setSAS(false);
            星舰.getControl().setRCS(true);
            星舰.getControl().setThrottle(1);//推力级别 0~1

            boolean if第一阶段垂直发射=false;
            boolean if第一阶段程序转向=false;
            boolean if第一阶段分离=false;
            boolean if第二阶段结束=false;
            boolean if第三阶段结束=false;
            //第一阶段 垂直发射
            {
                //////测试区

                //////
                Thread.sleep(1000);
                System.out.println("启动");
                星舰.getControl().activateNextStage();//空格键
                星舰.getAutoPilot().setAutoTune(true);
                星舰.getAutoPilot().setSASMode(STABILITY_ASSIST);
                星舰.getAutoPilot().engage();//启动了火箭的自动驾驶仪
                星舰.getAutoPilot().targetPitchAndHeading(90, 90);//俯仰角（pitch）和航向角（heading）
                Thread.sleep(10000);
                if第一阶段垂直发射=true;
            }
            //第一阶段 程序转向
          /*  {
                int 转向角=1;
                boolean if第一阶段程序转向转向阶段=false;
                System.out.println("///等待高度进入8KM，进行程序转向///");
                while (if第一阶段垂直发射==true)
                {
                    int 高度= (int) 星舰.flight(参考系).getMeanAltitude();
                    while (高度 > 8000)//获取高度
                    {
                        星舰.getAutoPilot().targetPitchAndHeading(90-转向角, 90);//慢慢转向
                        //System.out.println("转向角 "+转向角);
                        转向角++;
                        Thread.sleep(2000);//每次转一度需要的时间
                        if (转向角==10)//需要转向的角度大小
                        {
                            System.out.println("转向结束,倾角80");
                            if第一阶段程序转向转向阶段=true;
                            break;
                        }
                    }
                    Thread.sleep(500);//循环刷新时间
                    if (if第一阶段程序转向转向阶段)
                    {
                        System.out.println("///第一阶段 程序转向结束///");
                        if第一阶段程序转向=true;
                        break;
                    }
                }
            }  */
            //第一阶段 分离
            {
                System.out.println("等待一级燃烧至20S剩余");
                while (if第一阶段垂直发射)
                {

                    Thread.sleep(200);//刷新频率
                    if (星舰.resourcesInDecoupleStage(3, false).amount("LiquidFuel") < 4092) //即为20秒剩余 20/95*19440
                    {
                        星舰.getControl().setThrottle(0);
                        Thread.sleep(3000);
                        星舰.getControl().activateNextStage();//空格键 分离一二级
                        System.out.println("///第一阶段 分离///");
                        Thread.sleep(3000);
                        星舰.getControl().setThrottle(0.2f);//二级启动
                        Thread.sleep(3000);
                        星舰.getControl().activateNextStage();//抛整流罩
                        Thread.sleep(1000);
                        星舰.getControl().setThrottle(1);//全功率运行
                        if第一阶段分离=true;
                        break;
                    }
                }
            }
            //第二阶段 AP达到同步轨道高度
            {
                System.out.println("///第二阶段AP达到同步轨道高度///");
                boolean 二阶段接近同步轨道=false;
                boolean 二阶段到达同步轨道=false;
                while(if第一阶段分离)
                {
                    Thread.sleep(500);
                    if (星舰.getOrbit().getApoapsis()>2500000+ 600080)
                    {
                        星舰.getControl().setThrottle(0.1f);
                        System.out.println("接近同步轨道，降低引擎功率");
                        System.out.println("当前Ap "+(星舰.getOrbit().getApoapsis()-600080));
                        二阶段接近同步轨道=true;
                        break;
                    }
                }
                while (二阶段接近同步轨道)
                {
                    Thread.sleep(200);
                    if (星舰.getOrbit().getApoapsis()>2863330+ 600080)
                    {
                        星舰.getControl().setThrottle(0);
                        System.out.println("///到达同步轨道///");
                        System.out.println("当前Ap "+(星舰.getOrbit().getApoapsis()-600080));
                        二阶段到达同步轨道=true;
                        break;
                    }
                }
                System.out.println("当前倾角弧度: "+星舰.getOrbit().getInclination());
                System.out.println("///开始调整倾角///");
                星舰.getAutoPilot().targetPitchAndHeading(0, 315);
                Thread.sleep(6000);
                星舰.getControl().setThrottle(0.1f);
                while (二阶段到达同步轨道)
                {
                    Thread.sleep(100);
                    //System.out.println("当前倾角弧度: "+ 星舰.getOrbit().getInclination());
                    if (星舰.getOrbit().getInclination()>1.5697)
                    {
                        星舰.getControl().setThrottle(0);
                        星舰.getAutoPilot().targetPitchAndHeading(0, 0);
                        System.out.println("倾角调整完毕,当前倾角弧度（1.5707参考）: "+星舰.getOrbit().getInclination());
                        System.out.println("///即将转移至同步轨道卫星--着陆器///");
                        Thread.sleep(2000);
                        if第二阶段结束=true;
                        break;
                    }
                }
            }
            //第三阶段 着陆器姿态调整
            {
                boolean if着陆器查找=false;
                boolean if着陆器切换=false;
                boolean if水平速度增加=false;
                //查找着陆器索引
                while (if第二阶段结束)
                {
                    List<SpaceCenter.Vessel> 活跃的飞船列表=太空中心.getVessels();//读取飞船列表
                    for (int i = 0; i < 活跃的飞船列表.size(); i++)
                    {
                        if (活跃的飞船列表.get(i).getName().equals("同步轨道卫星--着陆器"))
                        {//名称判断
                             System.out.println("同步轨道卫星--着陆器 索引是 "+i+" 正在切换");//如果是，输出并且切换至该舰体
                            太空中心.setActiveVessel(活跃的飞船列表.get(i));
                            if着陆器查找=true;
                             break;
                        }
                    }
                    if (if着陆器查找)
                    {
                        if着陆器切换=true;
                        break;
                    }
                }
                //切换着陆器
                着陆器=太空中心.getActiveVessel();
                着陆器.getControl().setSAS(false);
                着陆器.getControl().setRCS(true);
                着陆器.getAutoPilot().setAutoTune(true);
                着陆器.getAutoPilot().engage();
                着陆器.getAutoPilot().targetPitchAndHeading(0, 90);
                //加大水平速度
                System.out.println("正在调整倾角(0)与航向(90)");
                while (if着陆器切换)
                {
                    Thread.sleep(1000);
                    着陆器.getAutoPilot().targetPitchAndHeading(0, 90);//水平姿态
                    if (着陆器.flight(参考系).getPitch()<=2&&((int)着陆器.flight(参考系).getHeading()==92||(int)着陆器.flight(参考系).getHeading()==88))
                    {
                        Thread.sleep(3000);
                        System.out.println("调整完毕，准备点火");
                        着陆器.getControl().setThrottle(0.5f);//加大水平速度
                        Thread.sleep(2980);
                        着陆器.getControl().setThrottle(0);//298秒后熄火
                        if水平速度增加=true;
                        break;
                    }
                }
                //调整经纬度
                boolean if倾角调整=false;
                while (if水平速度增加)
                {
                    Thread.sleep(1000);
                   double 着陆器经度=着陆器.flight(参考系).getLatitude();
                    System.out.println("///开始调整经度///");
                    System.out.println("着陆器经度："+着陆器经度);
                    System.out.println("着陆器倾角弧度："+着陆器.getOrbit().getInclination());
                    /// ///正负两个if
                    if(着陆器.getOrbit().getInclination()<0.025)
                    {
                        System.out.println("基本对齐");
                        System.out.println("///着陆器姿态调整结束,等待下落///");
                        if第三阶段结束=true;
                        break;
                    }
                    /// ///正负两个if
                    if (if倾角调整)
                    {
                        着陆器.getControl().setThrottle(0.02f);
                        Thread.sleep(3000);
                        着陆器.getControl().setThrottle(0);
                        System.out.println("倾角调整完毕，引擎关闭,当前倾角弧度: "+着陆器.getOrbit().getInclination());
                        System.out.println("///着陆器姿态调整结束,等待下落///");
                        if第三阶段结束=true;
                        break;
                    }
                    /// ///判断经度正负
                   if (着陆器经度<0)//判断经度负
                   {
                       System.out.println("经度<0,航向调整至0");
                       着陆器.getAutoPilot().targetPitchAndHeading(0, 0);//调整姿态
                       while (true)
                       {
                           Thread.sleep(200);
                           int 航向= (int) 着陆器.flight(参考系).getHeading();
                         if (航向<=2)
                          {
                              Thread.sleep(500);
                              着陆器.getControl().setThrottle(0.02f);//调整经度
                              System.out.println(着陆器.getOrbit().getInclination());//获取倾角
                           if (着陆器.getOrbit().getInclination()<0.01)
                           {
                               Thread.sleep(6000);
                               着陆器.getControl().setThrottle(0);
                               if倾角调整=true;
                               break;
                           }
                         }
                       }
                   }
                    if (着陆器经度>0)//判断经度正
                    {
                        System.out.println("经度>0,航向调整至180");
                        着陆器.getAutoPilot().targetPitchAndHeading(0, 180);//调整姿态
                        while (true)
                        {
                            Thread.sleep(200);
                            int 航向= (int) 着陆器.flight(参考系).getHeading();
                            if (航向>=178)
                            {
                                Thread.sleep(200);
                                着陆器.getControl().setThrottle(0.02f);//调整经度
                                System.out.println(着陆器.getOrbit().getInclination());//获取倾角
                                if (着陆器.getOrbit().getInclination()<0.01)
                                {
                                    Thread.sleep(6000);
                                    着陆器.getControl().setThrottle(0);
                                    if倾角调整=true;
                                    break;
                                }
                            }
                        }
                    }
                    /// ///判断经度正负!!!
                }

            }
            //第四阶段 一级着陆
            {
                boolean if水平速度抵消=false;
                boolean if一次垂直减速=false;
                boolean if缓慢着陆=false;
                着陆器.getAutoPilot().targetPitchAndHeading(100, 90);//调整姿态

                while (if第三阶段结束)//准备抵消水平速度
                {
                    //System.out.println(着陆器.flight(参考系).getBedrockAltitude());//11.68
                    Thread.sleep(500);
                    if (着陆器.flight(参考系).getSurfaceAltitude()<20000)
                    {
                        着陆器.getControl().setBrakes(true);
                        着陆器.getAutoPilot().targetPitchAndHeading(110, 90);//调整姿态
                        着陆器.getControl().setActionGroup(2,true);//动作组2，只开一个引擎
                        着陆器.getControl().setThrottle(1);
                       // System.out.println(着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getHorizontalSpeed());
                        if ((着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getHorizontalSpeed()<=4))
                        {
                            着陆器.getControl().setActionGroup(2,false);//动作组1，全开引擎
                            System.out.println("水平速度抵消，回正姿态");
                            着陆器.getControl().setThrottle(0);
                            着陆器.getAutoPilot().targetPitchAndHeading(91, 90);
                            if水平速度抵消=true;
                            break;
                        }
                    }
                }
                double thrust;
                double gravity = 9.8;
                double 减速度;
                while (if水平速度抵消)
                {
                    着陆器.getControl().setLegs(true);
                    Thread.sleep(100);
                    //System.out.println(着陆器.flight(参考系).getSurfaceAltitude());
                    thrust = 着陆器.getAvailableThrust() * 0.7;
                    减速度 = (thrust - 着陆器.getMass() * gravity) / 着陆器.getMass();
                    double 高度 = 着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() * 着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() / (2 * 减速度);
                    if (着陆器.flight(参考系).getSurfaceAltitude() - 高度 < 20) {
                        System.out.println("第二次点火高度 "+ 高度);
                        着陆器.getControl().setThrottle(0.7f);
                        if一次垂直减速 = true;
                        break;
                    }
                }
                //下落速度第二次回到0左右，进入下一阶段
                while (if一次垂直减速) {
                    Thread.sleep(50);
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    if (当前垂直速度 > -5) {
                        float 平衡推力 = 着陆器.getMass() * 10 / (着陆器.getAvailableThrust());
                        着陆器.getControl().setThrottle( (平衡推力));
                        if缓慢着陆 = true;
                        System.out.println("缓慢着陆");
                        break;
                    }
                }
                //缓慢着陆
                while (if缓慢着陆) {
                    Thread.sleep(100);
                    着陆器.getAutoPilot().targetPitchAndHeading(90, 90);//左右角
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    float 平衡推力 = 着陆器.getMass() * 10 / (着陆器.getAvailableThrust());
                    // System.out.println(平衡推力);
                    if (当前垂直速度 < -2) {
                        着陆器.getControl().setThrottle((float) (平衡推力*1.2));
                    } else if (当前垂直速度 > -1) {
                        着陆器.getControl().setThrottle((float) (平衡推力 * 0.5));
                    }
                    if (着陆器.flight(参考系).getSurfaceAltitude() < 11.88) {
                        着陆器.getControl().setThrottle(0);
                        System.out.println("着陆");
                        //if完成着陆=true;
                        break;
                    }
                }

        }

    }
}
}
