package org.program;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;

import static krpc.client.services.SpaceCenter.SASMode.STABILITY_ASSIST;

public class 一级发射程序 {
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        Connection 游戏连接 = Connection.newInstance("一级发射程序");
        SpaceCenter 太空中心 = SpaceCenter.newInstance(游戏连接);
        SpaceCenter.Vessel 着陆器 = 太空中心.getActiveVessel();

        // 设置遥测流
        // 用于获取当前的通用时间
        太空中心.getUT();
        //持续监听SpaceCenter对象的getUT方法返回值的变化
        Stream<Double> 世界时间 = 游戏连接.addStream(SpaceCenter.class, "getUT");
        //当前飞船所在星球表面的 参考系
        SpaceCenter.ReferenceFrame 参考系 = 着陆器.getSurfaceReferenceFrame();
        //获取了当前飞船的 飞行状态，包括位置、速度等信息
        SpaceCenter.Flight 飞行状态 = 着陆器.flight(参考系);
        //持续监听飞船的 平均高度 的变化。getMeanAltitude方法返回飞船在其轨道上的平均高度。
        Stream<Double> 平均高度 = 游戏连接.addStream(飞行状态, "getMeanAltitude");
        //监听飞船轨道上 远地点 的高度变化
        Stream<Double> 远地点 =
                游戏连接.addStream(着陆器.getOrbit(), "getApoapsisAltitude");

        //=======================发射阶段============================
        着陆器.getControl().setSAS(false);
        着陆器.getControl().setRCS(false);
        //星舰.getControl().setThrottle(0);//推力级别 0~1
        着陆器.getAutoPilot().setAutoTune(true);
        着陆器.getAutoPilot().setSASMode(STABILITY_ASSIST);
        着陆器.getAutoPilot().engage();
        着陆器.getControl().activateNextStage();
        着陆器.getControl().setThrottle(1);
        着陆器.getAutoPilot().targetPitchAndHeading(90, 90);//调整姿态


        //往上飞行20秒后程序转弯
        {
            System.out.println("20秒后开始调整倾角");
            Thread.sleep(20000);
            float 调整角度= 0.2f;
            while (true)
            {
                //System.out.println(着陆器.getAutoPilot().getTargetPitch());
                Thread.sleep(200);
                //调整倾角直到45度
                if (调整角度<45)
                {
                    着陆器.getAutoPilot().targetPitchAndHeading(90 - 调整角度, 90);
                    调整角度 += 0.2f;
                }
                //查询燃料剩余，关闭引擎并分离=================
                if (着陆器.resourcesInDecoupleStage(3, false).amount("LiquidFuel") < 2660) //即为13秒剩余 13/95*19440
                {
                    System.out.println("燃料剩余13秒，关闭引擎。");
                    着陆器.getControl().setThrottle(0);
                    Thread.sleep(2000);
                    //分级，低功率分离
                    System.out.println("二级分离3S后全功率运行35S");
                    着陆器.getControl().activateNextStage();
                    着陆器.getAutoPilot().targetPitchAndHeading(45, 90);
                    Thread.sleep(2000);
                    着陆器.getControl().setThrottle(0.1f);
                    Thread.sleep(3000);
                    着陆器.getAutoPilot().targetPitchAndHeading(30, 90);//调整姿态
                    着陆器.getControl().setThrottle(1);
                    Thread.sleep(35000);//持续点火35s后关火
                    着陆器.getControl().setThrottle(0);
                    System.out.println("关闭引擎");
                    break;
                }

            }
        }
        {
            着陆器.getAutoPilot().disengage();
            着陆器.getControl().setSAS(true);
            着陆器.getControl().setRCS(false);
            System.out.println("///发射阶段程序结束///");
            游戏连接.close();
        }



    }
}
