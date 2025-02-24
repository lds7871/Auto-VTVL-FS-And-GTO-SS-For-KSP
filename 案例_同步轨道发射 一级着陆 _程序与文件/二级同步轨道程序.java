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

public class 二级同步轨道程序 {
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        Connection 游戏连接 = Connection.newInstance("二级同步轨道程序");
        SpaceCenter 太空中心 = SpaceCenter.newInstance(游戏连接);
        SpaceCenter.Vessel 卫星 = 太空中心.getActiveVessel();
        // 设置遥测流
        // 用于获取当前的通用时间
        太空中心.getUT();
        //持续监听SpaceCenter对象的getUT方法返回值的变化
        Stream<Double> 世界时间 = 游戏连接.addStream(SpaceCenter.class, "getUT");
        //当前飞船所在星球表面的 参考系
        SpaceCenter.ReferenceFrame 参考系 = 卫星.getSurfaceReferenceFrame();
        //获取了当前飞船的 飞行状态，包括位置、速度等信息
        SpaceCenter.Flight 飞行状态 = 卫星.flight(参考系);

//=======================同步轨道升高阶段============================
//第一阶段 查找切换至着陆器
        {
            List<SpaceCenter.Vessel> 活跃的飞船列表=太空中心.getVessels();//读取飞船列表
            for (int i = 0; i < 活跃的飞船列表.size(); i++)
            {
                if (活跃的飞船列表.get(i).getName().equals("（可回收）芯凪一号"))
                {//名称判断
                    System.out.println("（可回收）芯凪一号 索引是 "+i+" 正在切换");//如果是，输出并且切换至该舰体
                    太空中心.setActiveVessel(活跃的飞船列表.get(i));
                    break;
                }
            }
        }

        //配置同步轨道信息

        卫星=太空中心.getActiveVessel();
        卫星.getControl().setSAS(false);
        卫星.getControl().setRCS(true);
        //卫星.getControl().setThrottle(0);//推力级别 0~1
        卫星.getAutoPilot().setAutoTune(true);
        卫星.getAutoPilot().setSASMode(STABILITY_ASSIST);
        卫星.getAutoPilot().engage();
        卫星.getAutoPilot().targetPitchAndHeading(30, 90);//调整姿态
        Thread.sleep(3000);
        boolean if姿态调整完毕=true;

//第二阶段 AP达到同步轨道高度
        boolean if二阶段完成=false;
        {
            System.out.println("///第二阶段AP达到同步轨道高度///");
            boolean 二阶段接近同步轨道=false;
            boolean 二阶段AP达到同步轨道高度=false;
            卫星.getControl().activateNextStage();//分级 抛整流罩
            Thread.sleep(5000);
            卫星.getControl().setThrottle(1);//引擎 1

            //判断接近同步轨道
            while(if姿态调整完毕)
            {
                Thread.sleep(500);
                if (卫星.getOrbit().getApoapsis()>2500000+ 600080)
                {
                    卫星.getControl().setThrottle(0.05f);//引擎 0.05
                    System.out.println("接近同步轨道，降低引擎功率");
                    System.out.println("当前Ap "+(卫星.getOrbit().getApoapsis()-600080));
                    二阶段接近同步轨道=true;
                    break;
                }
            }
            //判断AP达到同步轨道高度
            while (二阶段接近同步轨道)
            {
                Thread.sleep(200);
                if (卫星.getOrbit().getApoapsis()>2863330+ 600080)
                {
                    卫星.getControl().setThrottle(0);//引擎 0
                    System.out.println("///到达同步轨道///");
                    System.out.println("当前Ap "+(卫星.getOrbit().getApoapsis()-600080));
                    Thread.sleep(1000);
                    二阶段AP达到同步轨道高度=true;
                    break;
                }
            }
            //分级，姿态调整
            while (二阶段AP达到同步轨道高度)
            {
                卫星.getControl().activateNextStage();//分级 分离
                Thread.sleep(2000);
                卫星.getAutoPilot().targetPitchAndHeading(-1, 90);//调整姿态
                if二阶段完成=true;
                break;
            }
        }
//三阶段 点火画圆
        Thread.sleep(2000);
        System.out.println("等待接近远地点");
        boolean if三阶段完成=false;
        {
            boolean if到达远地点=false;
            //判断到达远地点
            while (if二阶段完成)
            {
                Thread.sleep(1000);
                System.out.println("与远地点的差值"+(卫星.getOrbit().getApoapsisAltitude()-卫星.flight(参考系).getBedrockAltitude()));//当前高度与远地点的差值
                if(Math.abs(卫星.getOrbit().getApoapsisAltitude()-卫星.flight(参考系).getBedrockAltitude())<30)//远地点高度减去当前高度小于25
                {
                    System.out.println("///第三阶段远地点点火///");
                    if到达远地点=true;
                    卫星.getControl().activateNextStage();//分级 点火
                    卫星.getControl().setThrottle(1);//点火
                    break;
                }
            }
            //判断近地点高度接近同步轨道高度
            int k=0;
            while (if到达远地点)
            {
                Thread.sleep(500);
                System.out.println("当前近地点"+卫星.getOrbit().getPeriapsisAltitude());
                //ed达到高度，关火
                if (卫星.getOrbit().getPeriapsisAltitude()>2863330)
                {
                    卫星.getControl().setThrottle(0);
                    System.out.println("近地点达到高度,关闭引擎，调整姿态");
                    卫星.getAutoPilot().targetPitchAndHeading(-90, 90);
                    Thread.sleep(7000);
                    卫星.getControl().toggleActionGroup(1);
                    if三阶段完成=true;
                    break;
                }
                //ed接近高度，降低功率
                if (卫星.getOrbit().getPeriapsisAltitude()>2500000)
                {
                    if (k==0){System.out.println("近地点接近同步轨道，降低引擎功率");k++;}
                    卫星.getAutoPilot().targetPitchAndHeading(-35, 90);//调整姿态
                    卫星.getControl().setThrottle(0.05f);//引擎 0.05
                    System.out.println("当前PE "+(卫星.getOrbit().getPeriapsisAltitude()));
                }

            }


        }
//程序完成
        if (if三阶段完成)
        {
            游戏连接.close();
            System.out.println("程序完成，关闭游戏连接");

        }

    }
}
