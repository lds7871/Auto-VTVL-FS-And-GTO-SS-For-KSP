package org.program;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;
import java.util.List;

import static krpc.client.services.SpaceCenter.SASMode.RADIAL;
import static krpc.client.services.SpaceCenter.SASMode.STABILITY_ASSIST;

public class 一级着陆程序 {
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        Connection 游戏连接 = Connection.newInstance("一级着陆程序");
        SpaceCenter 太空中心 = SpaceCenter.newInstance(游戏连接);
        SpaceCenter.Vessel 着陆器= 太空中心.getActiveVessel();

// 设置遥测流
        // 用于获取当前的通用时间
        太空中心.getUT();
        //持续监听SpaceCenter对象的getUT方法返回值的变化
        Stream<Double> 世界时间 = 游戏连接.addStream(SpaceCenter.class, "getUT");
        //当前飞船所在星球表面的 参考系
        SpaceCenter.ReferenceFrame 参考系 = 着陆器.getSurfaceReferenceFrame();
        //获取了当前飞船的 飞行状态，包括位置、速度等信息
        SpaceCenter.Flight 飞行状态 = 着陆器.flight(参考系);

//=======================着陆阶段============================
        //查找切换至着陆器
        {
           List<SpaceCenter.Vessel> 活跃的飞船列表=太空中心.getVessels();//读取飞船列表
           for (int i = 0; i < 活跃的飞船列表.size(); i++)
           {
               if (活跃的飞船列表.get(i).getName().equals("同步轨道卫星--着陆器"))
               {//名称判断
                   System.out.println("同步轨道卫星--着陆器 索引是 "+i+" 正在切换");//如果是，输出并且切换至该舰体
                   太空中心.setActiveVessel(活跃的飞船列表.get(i));
                   break;
               }
           }
        }

        //配置着陆器

            着陆器=太空中心.getActiveVessel();
            着陆器.getControl().setSAS(false);
            着陆器.getControl().setRCS(true);
            //星舰.getControl().setThrottle(0);//推力级别 0~1
            着陆器.getAutoPilot().setAutoTune(true);
            着陆器.getAutoPilot().setSASMode(STABILITY_ASSIST);
            着陆器.getAutoPilot().engage();
            着陆器.getAutoPilot().targetPitchAndHeading(110, 90);//调整姿态

        boolean if完成着陆=false;
        //着陆代码块
        {
            boolean if水平速度抵消=false;
            boolean if一次垂直减速=false;
            boolean if缓慢着陆=false;

            System.out.println("///等待低于两万米开始抵消水平速度///");
            while (true)//准备抵消水平速度
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
                //System.out.println(着陆器.flight(参考系).getSurfaceAltitude());//输出当前离地高度
                Thread.sleep(100);
                着陆器.getAutoPilot().targetPitchAndHeading(90, 90);//左右角
                double 当前垂直速度 = Double.parseDouble(String.valueOf(着陆器.flight(着陆器.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                float 平衡推力 = 着陆器.getMass() * 10 / (着陆器.getAvailableThrust());
                // System.out.println(平衡推力);
                if (当前垂直速度 < -2) {
                    着陆器.getControl().setThrottle(平衡推力*1);
                } else if (当前垂直速度 > -1) {
                    着陆器.getControl().setThrottle((float) (平衡推力 * 0.5));
                }
                if (着陆器.flight(参考系).getSurfaceAltitude() < 11.5)//高度修改
                {
                    着陆器.getControl().setThrottle(0);
                    System.out.println("///着陆完成///");
                    if完成着陆=true;
                    break;
                }
            }

        }
        //关闭自动控制，开启sas
        {
            while (if完成着陆)
            {
                Thread.sleep(3000);
                着陆器.getControl().setSASMode(STABILITY_ASSIST);
                System.out.println("程序关闭");
                游戏连接.close();
                break;
            }
        }

}}
