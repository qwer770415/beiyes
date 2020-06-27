/**
 *  Copyright 2016 ChinaSoft International Ltd. All rights reserved.
 */
package com.chinasofti.spamdetermination;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>
 * Title: BayesUtil
 * </p>
 * <p>
 * Description:利用贝叶斯分类判定垃圾信息的核心工具
 * </p>
 * <p>
 * Copyright: Copyright (c) 2016
 * </p>
 * <p>
 * Company: ChinaSoft International Ltd.
 * </p>
 * 
 * @author etc
 * @version 1.0
 */
public class BayesUtil {

	/**
	 * 合法信息数量
	 */
	private int hamNum;
	/**
	 * 垃圾信息数量
	 */
	private int spamNum;

	/**
	 * 合法信息字符串集
	 */
	private ArrayList<String> hams = new ArrayList<String>();
	/**
	 * 非法信息字符串结
	 */
	private ArrayList<String> spams = new ArrayList<String>();

	/**
	 * 保存分词结果对应出现在合法信息和非法信息中的次数的Map集合， WordInfo类保存了单一的分词结果在非法信息和和合法信息中分别出现的次数
	 */
	private HashMap<String, WordInfo> infoMap = new HashMap<String, WordInfo>();

	/**
	 * 消息中出现特定字符串出现在合法信息中的比
	 */
	private HashMap<String, Float> wordHamPossibility = new HashMap<String, Float>();
	/**
	 * 消息中出现特定字符串出现在非法信息中的比
	 */
	private HashMap<String, Float> wordSpamPossibility = new HashMap<String, Float>();

	/**
	 * 加载贝叶斯分类需要的学习数据
	 * 
	 * @param trainingDataFile 为贝叶斯分类判定垃圾信息提供的学习数据文
	 */
	public void loadTrainingData(File trainingDataFile) {
		// 尝试加载学习数据文件
		try {
			// 针对学习数据文件构建缓存的字符流，利用其可以采用行的方式读取学习数据
			BufferedReader fileReader = new BufferedReader(new FileReader(trainingDataFile));
			// 定义按照行的方式读取学习数据的临时变
			String data = "";
			// 循环读取学习文件中的数据
			while ((data = fileReader.readLine()) != null) {
				// 按照格式分割字符串，将会分割成两部分，第部分为ham或spam,用于说明本行数据是有效消息还是垃圾消息，第二部分为消息体本身
				String[] datas = data.split("	");
				// 对消息体本身进行单分词（本学习数据均为英文数据，因此可以利用空格进行自然分词，但是直接用空格分割还是有些单粗暴，因为没有处理标点符号，大家可以对其进行扩展，先用正则表达式处理标点符号后再进行分词，也可以扩展加入中文的分词功能
				String[] words = datas[1].split(" ");
				// 判定本条消息是否为有效消
				if ("ham".equals(datas[0])) {
					// 如果是有效消息，则将其加入有效消息集
					hamNum++;
					hams.add(datas[1]);
					// 遍历消息的分词结
					for (String word : words) {
						// 如果保存单次的集合中已经存在该单
						if (infoMap.containsKey(word)) {
							// 则将该单次在有效消息中出现的次数1
							infoMap.get(word).setHamNum(infoMap.get(word).getHamNum() + 1);
							// 如果以前没有出现过该单词
						} else {
							// 创建信息的单词描述信息对
							WordInfo info = new WordInfo();
							// 设置单词本体
							info.setWord(word);
							// 设置在有效消息中出现的次数为1
							info.setHamNum(1);
							// 加入单词信息集合
							infoMap.put(word, info);
						}

					}
					// 如果该消息为垃圾消息
				} else {
					spamNum++;
					// 将其加入垃圾消息集合
					spams.add(datas[1]);
					// 循环遍历分词结果
					for (String word : words) {
						// 如果保存单次的集合中已经存在该单
						if (infoMap.containsKey(word)) {
							// 则将该单次在垃圾消息中出现的次数1
							infoMap.get(word).setSpamNum(infoMap.get(word).getSpamNum() + 1);
							// 如果以前没有出现过该单词
						} else {
							// 创建信息的单词描述信息对
							WordInfo info = new WordInfo();
							// 设置单词本体
							info.setWord(word);
							// 设置在垃圾消息中出现的次数为1
							info.setSpamNum(1);
							// 加入单词信息集合
							infoMap.put(word, info);
						}
					}
				}
			}
			// 输出学习数据读取完毕的调试信息，并输出分词后总共设计到的单词个数
			//System.out.println("读取完成" + infoMap.keySet().size());
			// 关闭文件读取
			fileReader.close();
			// 循环遍历分词后得到的每一个单
			for (String word : infoMap.keySet()) {
				// 计算如果存在该单词的话，是有效信息的比例
				float result = computeWordHamPossibility(word);
				// 将单词对应的有效信息比例存入集合
				wordHamPossibility.put(word, new Float(result));
				// 计算如果存在该单词的话，是垃圾信息的比例
				result = computeWordSpamPossibility(word);
				// 将单词对应的垃圾信息比例存入集合
				wordSpamPossibility.put(word, new Float(result));

			}
			// 获取有效信息的数
			// hamNum = hams.size();
			// 获取垃圾信息的数
			// spamNum = spams.size();
			// 捕获加载过程中可能出现的异常信息
		} catch (Exception ex) {
			// 如果存在异常信息则输
			ex.printStackTrace();
		}
		// 输出加载学习文件结束的调试信
		//System.out.println("加载结束");

	}

	/**
	 * 利用贝叶斯分类计算出现了特定单词的消息为有效消息的概率比例， 在计算中使用了拉普拉斯平滑处理（即将总体数目和有效信息存在的数目都加1，防止出0概率
	 * ），贝叶斯概率表达式：：P(B|A) = P(A|B)*P(B)/P(A)
	 * 
	 * @param word 要计算的单词
	 * @return 出现了该单词的消息为有效消息的概率比例（经过了拉普拉斯平滑处理）
	 */
	float computeWordHamPossibility(String word) {
		// 获取单词在有效信息中存在的次
		int wordHamNum = infoMap.get(word).getHamNum();
		int totalMsg = hamNum + spamNum;
		// 计算贝叶斯分类概率，+1:拉普拉斯平滑处理
		float result = ((float) wordHamNum / (float) (hamNum + 1)) // P(A|B)
				* ((float) (hamNum + 1) / (float) (totalMsg + 1)) // P(B)
				/ (((float) wordHamNum + 1) / (float) (totalMsg + 1));// P(A)
		// 返回计算结果
		return result;

	}

	/**
	 * 利用贝叶斯分类计算出现了特定单词的消息为垃圾消息的概率比例， 在计算中使用了拉普拉斯平滑处理（即将总体数目和有效信息存在的数目都加1，防止出0概率
	 * ），贝叶斯概率表达式：：P(B|A) = P(A|B)*P(B)/P(A)
	 * 
	 * @param word 要计算的单词
	 * @return 出现了该单词的消息为垃圾消息的概率比例（经过了拉普拉斯平滑处理）
	 */
	float computeWordSpamPossibility(String word) {
		// 获取单词在垃圾信息中存在的次
		int wordSpamNum = infoMap.get(word).getSpamNum();
		// 计算贝叶斯分类概率，+1:拉普拉斯平滑处理
		float result = ((float) wordSpamNum / (float) (spamNum + 1))
				* ((float) (spamNum + 1) / (float) (hamNum + spamNum + 1))
				/ (((float) wordSpamNum + 1) / (float) (hamNum + spamNum + 1));
		// 返回计算结果
		return result;

	}

	/**
	 * 计算字符串是有效信息的比率结
	 * 
	 * @param words 待计算字符串的分词结
	 * @return 字符串是有效信息的比率结
	 */
	float computeStringHamResult(String[] words) {
		// 定义结果变量
		float result = 1.0f;
		// 循环遍历目标字符串分词结
		for (String word : words) {
			// 如果单词存在于学习数据中
			if (infoMap.containsKey(word)) {
				// 累计出现该单词后消息为有效消息的比率
				result *= computeWordHamPossibility(word);
			}
		}
		// 返回计算结果
		return result;

	}

	/**
	 * 计算字符串垃圾信息的比率结果
	 * 
	 * @param words 待计算字符串的分词结
	 * @return 字符串是垃圾信息的比率结
	 */
	float computeStringSpamResult(String[] words) {
		// 定义结果变量
		float result = 1.0f;
		// 循环遍历目标字符串分词结
		for (String word : words) {
			// 如果单词存在于学习数据中
			if (infoMap.containsKey(word)) {
				// 累计出现该单词后消息为垃圾消息的比率
				result *= computeWordSpamPossibility(word);
			}
		}
		// 返回计算结果
		return result;

	}

	/**
	 * 判定个字符串是否垃圾信息字符串的判定方法
	 * 
	 * @param msg 待判定的目标字符
	 */
	public boolean isSpam(String msg) {
		// 对目标字符串进行自然分词
		String[] words = msg.split(" ");
		// 计算有效数据的比率和垃圾消息的比率，如果垃圾消息的比例更大，说明其为垃圾消息，
		// 反之则为有效消
		return computeStringSpamResult(words) > computeStringHamResult(words);

	}

	/**
	 * 工具测试方法
	 */
	public static void main(String[] args) {
		// 构建判定工具对象
		BayesUtil util = new BayesUtil();
		// 加载学习数据
		util.loadTrainingData(new File("SMSSpamCollection"));
		// 进行消息判定
		boolean result = util.isSpam("hello Free");
		System.out.println("hello Free = " + result);
		result = util.isSpam("I love you");
		// 输出判定结果
		System.out.println("I love you = " + result);
	}

}
