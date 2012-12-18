/*
Copyright (C) 2011  Diego Darriba, David Posada

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package es.uvigo.darwin.jmodeltest.observer;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import es.uvigo.darwin.jmodeltest.ApplicationOptions;
import es.uvigo.darwin.jmodeltest.ModelTest;
import es.uvigo.darwin.jmodeltest.io.TextOutputStream;
import es.uvigo.darwin.jmodeltest.model.Model;
import es.uvigo.darwin.jmodeltest.utilities.Utilities;

public class ConsoleProgressObserver implements Observer 
{
	private TextOutputStream stream;
	/** Timer for calculate the elapsed time **/
	private long startTime;
	private int totalModels;
	private int completedModels = 0;
	private boolean threadScheduling;
	private ModelTest modelTest;
	private ApplicationOptions options;

	public ConsoleProgressObserver(ModelTest modelTest) 
	{
		this.modelTest = modelTest;
		this.startTime = System.currentTimeMillis();
		this.stream = modelTest.getMainConsole();
		this.options = modelTest.getApplicationOptions();
		this.threadScheduling = options.threadScheduling;
	}

	@Override
	public void update(Observable o, Object arg) 
	{
		if (arg != null) 
		{
			ProgressInfo info = (ProgressInfo) arg;

			switch (info.getType()) 
			{
				case ProgressInfo.BASE_TREE_INIT:
					stream.println(" ");
					stream.println("Estimating a BIONJ-JC tree ... ");
					break;
	
				case ProgressInfo.BASE_TREE_COMPUTED:
					stream.println("OK");
					stream.println(info.getModel().getName() + " tree: "
							+ info.getModel().getTreeString());
					break;
	
				case ProgressInfo.OPTIMIZATION_INIT:
					this.totalModels = options.getNumModels();
					stream.println(" ");stream.println(" ");
					stream.println("::Progress::");
					stream.println(" ");
					stream.println("Model \t\t Exec. Time \t Total Time \t -lnL");
					stream.println("-------------------------------------------------------------------------");
					break;
					
				case ProgressInfo.GTR_OPTIMIZATION_INIT:
					stream.println("[Heuristic search] Optimizing " + info.getModel().getName() + " model");
					break;
			
				case ProgressInfo.GTR_OPTIMIZATION_COMPLETED:
					stream.println("[Heuristic search] Starting model filtering");
					break;
					
				case ProgressInfo.SINGLE_OPTIMIZATION_INIT:
					break;
	
				case ProgressInfo.SINGLE_OPTIMIZATION_COMPLETED:
					completedModels++;
					
					stream.print(String.format("%1$-10s\t", info.getModel().getName()));
					
					double modelLnL;
					if (info.getValue() == ProgressInfo.VALUE_REGULAR_OPTIMIZATION) 
					{
						modelLnL = info.getModel().getLnL();
					}
					else 
					{
						modelLnL = info.getModel().getLnLIgnoringGaps();
					}
					
					stream.print(info.getMessage() + "\t" 
							+ Utilities.calculateRuntime(startTime, System.currentTimeMillis()) + "\t" 
							+ String.format(Locale.ENGLISH, "%13.4f", modelLnL));
					
					if (ModelTest.MPJ_RUN && threadScheduling) 
					{
						stream.println(" ");
					}
					else 
					{
						if (info.isHeuristicSearch() && info.getValue() == ProgressInfo.VALUE_REGULAR_OPTIMIZATION) 
						{
							stream.println("\t [" + info.getHeuristicStage() + "/6] (" + completedModels + "/" + info.getNumModelsInStage() + ")");
							if (completedModels == info.getNumModelsInStage()) 
							{
								completedModels = 0;
							}
						}
						else 
						{
							stream.println("\t (" + completedModels + "/" + totalModels + ")");
						}
					}
					break;
				
				case ProgressInfo.REOPTIMIZATION_INIT:
					this.totalModels = info.getValue();
					this.completedModels = 0;
					this.startTime = System.currentTimeMillis();
					stream.println(" ");
					stream.println(" ");
					stream.println("Some models should be reoptimized for checking lnL against the unconstrained likelihood");
					stream.println(" ");
					stream.println("Model \t\t Exec. Time \t Total Time\t-lnL w/o gaps");
					stream.println("-------------------------------------------------------------------------");
					break;
						
				case ProgressInfo.REOPTIMIZATION_COMPLETED:
					stream.println(" ");
					stream.println("  Unconstrained -lnL       = " + options.getUnconstrainedLnL());
					stream.println("  Number of patterns found = " + options.getNumPatterns());
					stream.println(" ");
					break;
					
				case ProgressInfo.INTERRUPTED:
					stream.println(" ");
					stream.println("Computation of likelihood scores discontinued ...");
					break;
	
				case ProgressInfo.ERROR:
					stream.println(info.getMessage());
					stream.println(" ");
					stream.println("Computation of likelihood scores discontinued ...");
					System.exit(-1);
					break;
	
				case ProgressInfo.OPTIMIZATION_COMPLETED_OK:
					stream.println(" ");
					for (Model model : modelTest.getCandidateModels()) 
					{
						model.print(modelTest.getMainConsole());
						modelTest.getMainConsole().println(" ");
					}
	
					if (options.isAmbiguous()) 
					{
						stream.println("  Best-fit models should be reoptimized for comparison with unconstrained likelihood");
					}
					else 
					{
						stream.println("  Unconstrained -lnL       = " + options.getUnconstrainedLnL());
						stream.println("  Number of patterns found = " + options.getNumPatterns());
					}
					
					stream.println(" ");
					stream.println("Computation of likelihood scores completed. It took "
							+ Utilities.calculateRuntime(startTime, System.currentTimeMillis()) + ".");
					stream.println(" ");
					// continue
	
				case ProgressInfo.OPTIMIZATION_COMPLETED_INTERRUPTED:
					// dispose
					break;
			}
		}
	}

}
