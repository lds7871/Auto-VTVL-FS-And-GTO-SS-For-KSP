package org.program;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import java.io.IOException;

public class Main {

    private static double 参考着陆高度;
    private static double 远地点时间;

    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        Connection 游戏连接 = Connection.newInstance("火箭测试");
        SpaceCenter 太空中心 = SpaceCenter.newInstance(游戏连接);
        SpaceCenter.Vessel 火箭 = 太空中心.getActiveVessel();

// 设置遥测流
        // 用于获取当前的通用时间
        太空中心.getUT();
        //持续监听SpaceCenter对象的getUT方法返回值的变化
        Stream<Double> 世界时间 = 游戏连接.addStream(SpaceCenter.class, "getUT");
        //当前飞船所在星球表面的 参考系
        SpaceCenter.ReferenceFrame 参考系 = 火箭.getSurfaceReferenceFrame();
        //获取了当前飞船的 飞行状态，包括位置、速度等信息
        SpaceCenter.Flight 飞行状态 = 火箭.flight(参考系);
        //持续监听飞船的 平均高度 的变化。getMeanAltitude方法返回飞船在其轨道上的平均高度。
        Stream<Double> 平均高度 = 游戏连接.addStream(飞行状态, "getMeanAltitude");
        //监听飞船轨道上 远地点 的高度变化
        Stream<Double> 远地点 =
                游戏连接.addStream(火箭.getOrbit(), "getApoapsisAltitude");
        /*{
            //获取了飞船在分离第二阶段时的 资源情况
            SpaceCenter.Resources stage2Resources = 火箭.resourcesInDecoupleStage(2, false);
            //监听第二级分离阶段飞船中 固体燃料 的数量变化
            Stream<Float> 固体燃料 =
                    connection.addStream(stage2Resources, "amount", "SolidFuel");
        }*/

        //起飞阶段
        {
            参考着陆高度 = 火箭.flight(参考系).getBedrockAltitude();

            // 启动前设置
            火箭.getControl().setSAS(true);
            火箭.getControl().setRCS(true);
            火箭.getControl().setThrottle(0.8F);//推力级别 0~1


            //第一阶段
            Thread.sleep(1000);
            System.out.println("启动");
            火箭.getControl().activateNextStage();//空格键
            火箭.getAutoPilot().engage();//启动了火箭的自动驾驶仪
            火箭.getAutoPilot().targetPitchAndHeading(88.75F, 90);//俯仰角（pitch）和航向角（heading）


            boolean 发射阶段1 = false;
            //达到设定高度熄火
            while (true) {
                火箭.getOrbit().getTimeToApoapsis();//获取高度
                if (火箭.getOrbit().getApoapsis() > 100000 + 600080) {//100000
                    火箭.getControl().setThrottle(0);
                    远地点时间 = 火箭.getOrbit().getTimeToApoapsis();
                    System.out.println(远地点时间 + " 秒后达到远地点");
                    发射阶段1 = true;
                    break;
                }//设定远地点高度以及到达所需时间
            }
            //抛弃模拟载荷
            while (发射阶段1) {
                double 当前垂直速度 = Double.parseDouble(String.valueOf(火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                if (当前垂直速度 < 5) {
                    火箭.getControl().activateNextStage();
                    System.out.println("抛弃模拟载荷");
                    //获取数据参数输出
                    {
                        Thread.sleep(5000);
                        System.out.println("当前G力大小 " + 火箭.flight(参考系).getGForce());
                        System.out.println("当前质心海拔高度 " + 火箭.flight(参考系).getMeanAltitude());
                        System.out.println("当前地面高度 " + 火箭.flight(参考系).getElevation());
                        System.out.println("当前速度 " + 火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getSpeed());
                        System.out.println("当前垂直速度 " + 火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed());
                        System.out.println("当前身体表面以上地高度 " + 火箭.flight(参考系).getBedrockAltitude());
                    }
                    火箭.getAutoPilot().targetPitchAndHeading(91, 90);
                break;
                }
            }

        }
        //着陆阶段
        {

            boolean 着陆阶段0 = false;
            boolean 着陆阶段1 = false;
            boolean 着陆阶段2 = false;
            boolean 着陆阶段3 = false;
            boolean 着陆阶段4 = false;
            double thrust = 火箭.getAvailableThrust() * 0.8;
            double gravity = 9.8;
            double 减速度 = (thrust - 火箭.getMass() * gravity) / 火箭.getMass();
            //计算点火距离
            while (true) {
                double 当前垂直速度 = Double.parseDouble(String.valueOf(火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));

                if (Math.abs(当前垂直速度) > 300 && 当前垂直速度 < -300) {
                    火箭.getControl().setBrakes(true);
                }//打开减速板

                double 高度 = 火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() * 火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() / (2 * 减速度);
                if (火箭.flight(参考系).getBedrockAltitude() - 高度 < 10) {
                    着陆阶段0 = true;
                    System.out.println("第一次点火高度 "+ 高度);
                    System.out.println("着陆阶段0完成");
                    break;
                }
            }
            {
                //下落速度足够 火箭点火
                while (着陆阶段0) {
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    火箭.getAutoPilot().targetPitchAndHeading(92, 90);//反向姿态
                    System.out.println("反向倾斜");
                    if (当前垂直速度 < -50) {

                        火箭.getControl().setThrottle(0.8F);
                        着陆阶段1 = true;
                        System.out.println("着陆阶段1完成");
                        break;
                    }
                }
                //下落速度回到0左右，进入下一阶段
                while (着陆阶段1) {
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    if (当前垂直速度 > -6) {
                        火箭.getAutoPilot().targetPitchAndHeading(90, 90);//姿态回正
                        System.out.println("姿态回正");
                        火箭.getControl().setThrottle(0.3F);
                        Thread.sleep(300);
                        火箭.getControl().setThrottle(0);
                        着陆阶段2 = true;
                        System.out.println("着陆阶段2完成");
                        break;
                    }
                }
                //二次计算点火距离并着陆
                while (着陆阶段2) {
                    thrust = 火箭.getAvailableThrust() * 0.7;
                    减速度 = (thrust - 火箭.getMass() * gravity) / 火箭.getMass();
                    double 高度 = 火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() * 火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() / (2 * 减速度);
                    if (火箭.flight(参考系).getBedrockAltitude() - 高度 < 3) {
                        System.out.println("第二次点火高度 "+ 高度);
                        火箭.getControl().setThrottle(0.95F);
                        着陆阶段3 = true;
                        System.out.println("着陆阶段3完成");
                        break;
                    }

                }
                //下落速度第二次回到0左右，进入下一阶段
                while (着陆阶段3) {
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    if (当前垂直速度 > -1) {
                        火箭.getControl().setThrottle(0);
                        着陆阶段4 = true;
                        System.out.println("着陆阶段4完成");
                        break;
                    }
                }
                //缓慢着陆
                while (着陆阶段4) {
                    火箭.getAutoPilot().targetPitchAndHeading(90, 90);//左右角
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(火箭.flight(火箭.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    float 平衡推力 = 火箭.getMass() * 10 / (火箭.getAvailableThrust());
                    // System.out.println(平衡推力);
                    if (当前垂直速度 < -2) {
                        火箭.getControl().setThrottle(平衡推力);
                    } else if (当前垂直速度 > -1) {
                        火箭.getControl().setThrottle((float) (平衡推力 * 0.5));
                    }
                    if (火箭.flight(参考系).getBedrockAltitude() < 参考着陆高度 + 1) {
                        火箭.getControl().setThrottle(0);
                        System.out.println("着陆");
                        break;
                    }
                }


            }
        }
    }
}