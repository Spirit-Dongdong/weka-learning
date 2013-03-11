/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    C45ModelSelection.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.j48;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Utils;

import java.util.Enumeration;

/**
 * Class for selecting a C4.5-type split for a given dataset.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $
 */
public class C45ModelSelection extends ModelSelection {

	/** for serialization */
	private static final long serialVersionUID = 3372204862440821989L;

	/** Minimum number of objects in interval. 间隔内对象的最小个数，通常为2 */
	private int m_minNoObj;

	/** All the training data */
	private Instances m_allData; //

	/**
	 * Initializes the split selection method with the given parameters.
	 *
	 * @param minNoObj
	 *            minimum number of instances that have to occur in at least two
	 *            subsets induced by split
	 * @param allData
	 *            FULL training dataset (necessary for selection of split
	 *            points).
	 */
	public C45ModelSelection(int minNoObj, Instances allData) {
		m_minNoObj = minNoObj;
		m_allData = allData;
	}

	/**
	 * Sets reference to training data to null.
	 */
	public void cleanup() {

		m_allData = null;
	}

	/**
	 * Selects C4.5-type split for the given dataset.
	 */
	public final ClassifierSplitModel selectModel(Instances data) {

		double minResult;
//		double currentResult;
		C45Split[] currentModel;
		C45Split bestModel = null;
		NoSplit noSplitModel = null;
		double averageInfoGain = 0;
		int validModels = 0;
		boolean multiVal = true;
		Distribution checkDistribution;
		Attribute attribute;
		double sumOfWeights;
		int i;

		try {

			// Check if all Instances belong to one class or if not
			// enough Instances to split.检查无需分割的条件
			checkDistribution = new Distribution(data);
			noSplitModel = new NoSplit(checkDistribution);
			if (Utils.sm(checkDistribution.total(), 2 * m_minNoObj)//instance的个数小于2 * m_minNoObj（每个类至少有m_minNoObj个对象，要分成2个类）
					|| Utils.eq(checkDistribution.total(), checkDistribution
							.perClass(checkDistribution.maxClass())))//instance的总数和bag里面的class个数相等：所有instance都在一个class里了
				return noSplitModel;

			// Check if all attributes are nominal and have a
			// lot of values. 检查是否所有属性都是名词型（nominal），并且有很多值
			if (m_allData != null) {
				Enumeration enu = data.enumerateAttributes();
				while (enu.hasMoreElements()) {
					attribute = (Attribute) enu.nextElement();
					if ((attribute.isNumeric())//如果是数值型，multiVal为F
							|| (Utils.sm((double) attribute.numValues(),//如果不同值的个数<=实例总数*0.3，multiVal为F
									(0.3 * (double) m_allData.numInstances())))) {
						multiVal = false;
						break;
					}
				}
			}

			currentModel = new C45Split[data.numAttributes()];
			sumOfWeights = data.sumOfWeights();

			// For each attribute.
			/*
			 * 	对每个非类索引的属性：
			 * 		1.用C4.5对该属性进行分隔
			 * 		2.在该属性下构建分类器，这里会根据该属性是名称型（用枚举）还是数值型（用递归）
			 * 			数值型的情况见C45Split.handleNumericAttribute()

			 */



			for (i = 0; i < data.numAttributes(); i++) {

				// Apart from class attribute.
				if (i != (data).classIndex()) {

					// Get models for current attribute.
					currentModel[i] = new C45Split(i, m_minNoObj, sumOfWeights);
					currentModel[i].buildClassifier(data);

					// Check if useful split for current attribute
					// exists and check for enumerated attributes with
					// a lot of values.
					if (currentModel[i].checkModel())
						if (m_allData != null) {
							if ((data.attribute(i).isNumeric())
									|| (multiVal || Utils.sm((double) data
											.attribute(i).numValues(),
											(0.3 * (double) m_allData
													.numInstances())))) {
								averageInfoGain = averageInfoGain
										+ currentModel[i].infoGain();
								validModels++;
							}
						} else {
							averageInfoGain = averageInfoGain
									+ currentModel[i].infoGain();
							validModels++;
						}
				} else
					currentModel[i] = null;
			}

			// Check if any useful split was found.
			if (validModels == 0)
				return noSplitModel;
			averageInfoGain = averageInfoGain / (double) validModels;

			// Find "best" attribute to split on.
			minResult = 0;
			for (i = 0; i < data.numAttributes(); i++) {
				if ((i != (data).classIndex())
						&& (currentModel[i].checkModel()))

					// Use 1E-3 here to get a closer approximation to the
					// original
					// implementation.
					if ((currentModel[i].infoGain() >= (averageInfoGain - 1E-3))
							&& Utils.gr(currentModel[i].gainRatio(), minResult)) {
						bestModel = currentModel[i];
						minResult = currentModel[i].gainRatio();
					}
			}

			// Check if useful split was found.
			if (Utils.eq(minResult, 0))
				return noSplitModel;

			// Add all Instances with unknown values for the corresponding
			// attribute to the distribution for the model, so that
			// the complete distribution is stored with the model.
			bestModel.distribution().addInstWithUnknown(data,
					bestModel.attIndex());

			// Set the split point analogue to C45 if attribute numeric.
			if (m_allData != null)
				bestModel.setSplitPoint(m_allData);
			return bestModel;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Selects C4.5-type split for the given dataset.
	 */
	public final ClassifierSplitModel selectModel(Instances train,
			Instances test) {

		return selectModel(train);
	}

	/**
	 * Returns the revision string.
	 *
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 1.11 $");
	}
}
