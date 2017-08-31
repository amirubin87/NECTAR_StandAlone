package NECTAR;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConductanceMetricHandler implements ImetricHandler {
	
	// Mapping of community ID to nodes member in the community.
    public Map<Integer, ConductanceNode> conNodes;
    // Mapping of a node to the communities to which he belongs.
    public Map<Integer, ConductanceComm> conComms;
	
	public Map<Integer,Set<Integer>> neighbors;
	public Map<Integer,Set<Integer>> com2Nodes;	
	
	@Override
	public ImetricHandler deepCopy() {
		return this;
	}

	public ConductanceMetricHandler() {	}

	public void Init() {
		conNodes = new HashMap<Integer, ConductanceNode>();
    	conComms = new HashMap<Integer, ConductanceComm>();
		com2Nodes = new HashMap<Integer, Set<Integer>>();
	}

	@Override
	public void Init(UndirectedUnweightedGraph graph) {
		Init();

		neighbors = graph.neighbors;
		
		for (Integer node : graph.nodes()){
			ConductanceNode conNode = CreateConductanceNode(node, graph);
			conNodes.put(node, conNode);

			ConductanceComm conComm = CreateConductanceComm(node, graph);
			conComms.put(node, conComm);

			Set<Integer> comm = new HashSet<Integer>();
            comm.add(node);
            com2Nodes.put(node, comm);
        }
	}

	@Override
	public void Init(UndirectedUnweightedGraph graph, Map<Integer,Set<Integer>> firstPart) {
		Init(graph);

		// TODO: Amir improve perfomance
		for (Integer comm : firstPart.keySet()) {
			for (Integer node : firstPart.get(comm)) {
				UpdateRemoveNodeFromComm(node, node);
				UpdateAddNodeToComm(node, comm);
			}
		}
	}

	
	private ConductanceNode CreateConductanceNode(Integer node, UndirectedUnweightedGraph graph) {
		ConductanceNode conNode = new ConductanceNode(node, graph.neighbors.get(node).size());
		return conNode;
	}
	
	private ConductanceComm CreateConductanceComm(Integer node, UndirectedUnweightedGraph graph) {
		ConductanceComm conComm = new ConductanceComm(node, graph.neighbors.get(node).size());
		return conComm;
	}

	@Override
	public void UpdateRemoveNodeFromComm(Integer node, Integer comm) {
		ConductanceNode currNode = conNodes.get(node);
		Integer innerEdges = currNode.comm2InnerEdges.get(comm);
		Integer extEdges = currNode.comm2ExtEdges.get(comm);
		
		ConductanceComm currComm = conComms.get(comm);
		currComm.innerEdgesCount -= innerEdges;
		currComm.extEdgesCount += innerEdges;
		currComm.extEdgesCount -= extEdges;
		currComm.CalcMetric();
		
		for (Integer nd : com2Nodes.get(comm)) {
			if (neighbors.get(nd).contains(node)) {
				ConductanceNode currNd = conNodes.get(nd);
				currNd.comm2InnerEdges.put(comm, currNd.comm2InnerEdges.get(comm) - 1);
				currNd.comm2ExtEdges.put(comm, currNd.comm2ExtEdges.get(comm) + 1);
			}
		}
		
		com2Nodes.get(comm).remove(node);
		currNode.comm2InnerEdges.remove(comm);
		currNode.comm2ExtEdges.remove(comm);
	}

	@Override
	public void UpdateAddNodeToComm(Integer node, Integer comm) {
		ConductanceComm currComm = conComms.get(comm);

		// number of neighs in comm
		int count = 0;

		for (Integer nd : com2Nodes.get(comm)) {
			if (neighbors.get(nd).contains(node)) {
				count++;
				ConductanceNode currNd = conNodes.get(nd);
				currNd.comm2InnerEdges.put(comm, currNd.comm2InnerEdges.get(comm) + 1);
				currNd.comm2ExtEdges.put(comm, currNd.comm2ExtEdges.get(comm) - 1);
			}
		}	
		
		com2Nodes.get(comm).add(node);
		ConductanceNode currNode = conNodes.get(node);
		currNode.comm2InnerEdges.put(comm, count);
		currNode.comm2ExtEdges.put(comm, neighbors.get(node).size() - count);

		currComm.innerEdgesCount += count;
		currComm.extEdgesCount -= count;
		currComm.extEdgesCount += neighbors.get(node).size() - count;
		currComm.CalcMetric();
	}
	
	@Override
	public double CalcMetricImprovemant(Integer comm, Integer node) {
		ConductanceComm currComm = conComms.get(comm);
		double currMetric = currComm.metric;
		
		int count = 0;
		for (Integer nd : com2Nodes.get(comm)) {
			if (neighbors.get(nd).contains(node)) {
				count++;				
			}
		}
		
		Integer newInnerEdges = currComm.innerEdgesCount + count;
		Integer newExtEdges = currComm.extEdgesCount - count;
		newExtEdges += neighbors.get(node).size() - count;

		int denom = Math.max(1, (2*newInnerEdges + newExtEdges));
		double newMetric = (newExtEdges)/denom;
		return (currMetric - newMetric);
	}

}

class ConductanceComm
{
	public Integer id;
	
	public Integer innerEdgesCount;
	
	public Integer extEdgesCount;

	public double metric;
	
	public ConductanceComm(Integer id, Integer extEdgesCount) {
		this.id = id;
		this.innerEdgesCount = 0;
		this.extEdgesCount = extEdgesCount;
		this.metric = 0;
	}
	
	public void CalcMetric() {
		int denom = Math.max(1, (2*innerEdgesCount + extEdgesCount));
		metric = (extEdgesCount)/denom;
	}
}

class ConductanceNode
{
	public Integer id;
	
	public HashMap<Integer, Integer> comm2InnerEdges;
	
	public HashMap<Integer, Integer> comm2ExtEdges;
	
	public ConductanceNode(Integer id, Integer extEdgesCount) {
		this.id = id;
		comm2InnerEdges = new HashMap<Integer, Integer>();
		comm2InnerEdges.put(id, 0);
		
		comm2ExtEdges = new HashMap<Integer, Integer>();
		comm2ExtEdges.put(id, extEdgesCount);
	}
}
