import osmium as osm
import pandas as pd
import numpy as np
import tqdm
import scipy.io

class OSMHandler(osm.SimpleHandler):
    def __init__(self):
        osm.SimpleHandler.__init__(self)
        self.osm_node = []
        self.osm_way = []
        self.osm_data = []

    def node(self, n):
        self.osm_node.append([
                               n.id,
                               n.location.lat,
                               n.location.lon,
                             ])

    def way(self, w):
        nodes = []
        for node in w.nodes:
            nodes.append(node.ref)
        tags = {}
        for tag in w.tags:
            tags[tag.k] = tag.v
        if tags.get("highway") != None:
            self.osm_way.append([
                                   w.id,
                                   nodes,
                                   tags
                               ])
osmhandler = OSMHandler()
# scan the input file and fills the handler list accordingly
osmhandler.apply_file("edinburgh.osm")
# transform the list into a pandas DataFrame

node = osmhandler.osm_node
way = osmhandler.osm_way

print(len(node))
print(len(way))

node_colnames = ['id', 'lat', 'lon']
df_node = pd.DataFrame(node, columns=node_colnames)
df_node = df_node.drop_duplicates()

# sidewalk: both, left, right
# foot: yes, no
#for curway in range(0, len(way)):
#connectivity_matrix = np.zeros((len(node), len(node)), dtype=int)
connectivity_matrix = [[]*1 for i in range(len(node))]
for curway in tqdm.tqdm(range(len(way))):
    #current way node set
    nodeset = way[curway][1]
    nodes_num = len(nodeset)
    for node_local_index in range(0, nodes_num - 1):
        node_id = nodeset[node_local_index]
        #node_index = np.where(df_node['id'] == node_id)[0][0]
        node_index = df_node[df_node['id'] == node_id]['id'].keys()[0]

        neighbor_id = nodeset[node_local_index+1]
        neighbor_index = df_node[df_node['id'] == neighbor_id]['id'].keys()[0]
        #neighbor_index = np.where(df_node['id'] == neighbor_id)[0][0]

        if neighbor_index not in connectivity_matrix[node_index]:
            connectivity_matrix[node_index].append(neighbor_index)

        if node_index not in connectivity_matrix[neighbor_index]:
            connectivity_matrix[neighbor_index].append(node_index)

        #connectivity_matrix[node_index, neighbor_index] = 1
        #connectivity_matrix[neighbor_index, node_index] = 1

with open('adjacency_ed.txt', 'w') as f1:
    for i in tqdm.tqdm(range(len(connectivity_matrix))):
        #if len(connectivity_matrix[i]) > 0:
        f1.write( str(i) + ": ")
        for item in connectivity_matrix[i]:
            f1.write(str(item) +" ")
        f1.write("\n")
        f1.flush()
f1.close()
