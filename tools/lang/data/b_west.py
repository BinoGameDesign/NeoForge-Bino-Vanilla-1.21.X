def L(gem, noun, pat, raw, unique, tip, sub, hint, disc, of=None, gn=None):
    d = dict(gem=gem, noun=noun, pat=pat, raw=raw, unique=unique, tip=tip, sub=sub, hint=hint, disc=disc)
    if of: d['of'] = of
    if gn: d['gn'] = gn
    return d
def G(r, s, j, d): return dict(rubi=r, sapphire=s, jade=j, diamond=d)
def P(g, o, dp, b): return dict(gear=g, ore=o, deep=dp, block=b)
def R(d, r, s, j): return dict(diamond=d, rubi=r, sapphire=s, jade=j)
def U(fs, f, i, s, l, fo, pb, t): return dict(forge_stone=fs, ph_fragment=f, ph_incomplete=i, ph_small=s, ph_large=l, forge_ore=fo, ph_block=pb, table=t)
def T(*v): return dict(zip(['forge_stone_bonus','forge_stone_first','forge_stone_use','forged','diamond_tool','rubi_sword','rubi_pickaxe','rubi_axe','rubi_shovel','rubi_hoe','sapphire_tool','jade_tool','diamond_set','rubi_set','sapphire_set','jade_set','set_header'], v))
def S(*v): return dict(zip(['lapidary_spin','lapidary_ready','lapidary_done','lapidary_contact'], v))
def H(*v): return dict(zip(['take_flint','place_flint','take_ore','place_ore','press','working','wrong','leave','more','nothing'], v))
def D(*v): return dict(zip(['rubi_ore','sapphire_ore','jade_ore'], v))

DATA = {
 'es_es': L(G('Rubí','Zafiro','Jade','Diamante'),
  ['Espada','Pico','Pala','Hacha','Azada','Casco','Peto','Pantalones','Botas'],
  P('{n} de {g}','Mineral de {g}','Mineral de {g} de pizarra abisal','Bloque de {g}'),
  R('Diamante en bruto','Rubí en bruto','Zafiro en bruto','Jade en bruto'),
  U('Piedra de forja','Fragmento de piedra filosofal','Piedra filosofal incompleta','Piedra filosofal pequeña','Piedra filosofal grande','Mineral de forja','Bloque de piedra filosofal','Mesa de lapidario'),
  T('Forja: +%s%% en todos los atributos del equipo','Úsala en el yunque con un equipo sin forjar','Úsala en el yunque con un equipo +%s','Forja +%s: +%s%% en todos los atributos','Bonificación de diamante: +%s%% en todas las estadísticas','Velocidad de ataque +30%%','Rompe toda la veta de mineral (hasta 10 bloques)','Tala el árbol entero (hasta 64 troncos)','Rompe 3x3 bloques a la vez','Cosecha y replanta hasta 10 cultivos con clic derecho','1/%s de probabilidad de soltar 1 objeto extra','+%s%% de XP (bloques y mobs)','Velocidad de movimiento +%s%%','Esquiva: %s%% de probabilidad de ignorar un daño','Magnético: atrae objetos en %s bloques','Peso pluma: -%s%% de daño por caída','Conjunto de %s (%s/4)'),
  S('El disco del lapidario gira','Mesa de lapidario lista','Gema tallada','El lapidario raspa la piedra'),
  H('Coge un pedernal de la barra','Coloca el pedernal en la punta del lapidario','Coge un mineral en bruto de la barra','Coloca el mineral en el centro del disco','Pulsa el botón para tallar  (Mayús: tallar todo)','Tallando...','Ese objeto no va ahí','Esc: salir  ·  Clic derecho: devolver el objeto','Más objetos del inventario','No hay nada más que tallar en el inventario'),
  D('Rubí: cristal rojo de brillo intenso.','Zafiro: cristal azul, frío al tacto.','Jade: piedra verde y lisa, muy resistente.'),
  of=dict(rubi='rubí',sapphire='zafiro',jade='jade'), gn=dict(rubi='rubí',sapphire='zafiro',jade='jade')),

 'fr_fr': L(G('Rubis','Saphir','Jade','Diamant'),
  ['Épée','Pioche','Pelle','Hache','Houe','Casque','Plastron','Jambières','Bottes'],
  P('{n} en {g}','Minerai de {g}','Minerai de {g} des abîmes','Bloc de {g}'),
  R('Diamant brut','Rubis brut','Saphir brut','Jade brut'),
  U('Pierre de forge','Fragment de pierre philosophale','Pierre philosophale incomplète','Petite pierre philosophale','Grande pierre philosophale','Minerai de forge','Bloc de pierre philosophale','Table de lapidaire'),
  T("Forge : +%s %% à tous les attributs de l'équipement","À utiliser sur une enclume avec un équipement non forgé","À utiliser sur une enclume avec un équipement +%s",'Forge +%s : +%s %% à tous les attributs','Bonus de diamant : +%s %% à toutes les statistiques',"Vitesse d'attaque +30 %%","Casse toute la veine de minerai (jusqu'à 10 blocs)","Abat l'arbre entier (jusqu'à 64 troncs)","Casse 3x3 blocs d'un coup","Récolte et replante jusqu'à 10 cultures avec le clic droit",'1/%s de chance de lâcher 1 objet de plus',"+%s %% d'XP (blocs et créatures)",'Vitesse de déplacement +%s %%',"Esquive : %s %% de chances d'ignorer un dégât","Magnétique : attire les objets dans un rayon de %s blocs",'Poids plume : -%s %% de dégâts de chute','Ensemble %s (%s/4)'),
  S('Le disque du lapidaire tourne','Table de lapidaire prête','Gemme taillée','Le lapidaire ponce la pierre'),
  H('Prenez du silex dans la barre','Placez le silex sur la pointe du lapidaire','Prenez un minerai brut dans la barre','Placez le minerai au centre du disque','Appuyez sur le bouton pour tailler  (Maj : tout tailler)','Taille en cours...','Cet objet ne va pas là',"Échap : quitter  ·  Clic droit : reposer l'objet","Plus d'objets de l'inventaire","Rien d'autre à tailler dans l'inventaire"),
  D("Rubis : cristal rouge à l'éclat intense.",'Saphir : cristal bleu, froid au toucher.','Jade : pierre verte et lisse, très résistante.'),
  of=dict(rubi='rubis',sapphire='saphir',jade='jade'), gn=dict(rubi='rubis',sapphire='saphir',jade='jade')),

 'de_de': L(G('Rubin','Saphir','Jade','Diamant'),
  ['schwert','spitzhacke','schaufel','axt','hacke','helm','brustplatte','hose','stiefel'],
  P('{g}{n}','{g}erz','Tiefenschiefer-{g}erz','{g}block'),
  R('Rohdiamant','Rohrubin','Rohsaphir','Rohjade'),
  U('Schmiedestein','Fragment des Steins der Weisen','Unvollständiger Stein der Weisen','Kleiner Stein der Weisen','Großer Stein der Weisen','Schmiedeerz','Block des Steins der Weisen','Edelsteinschleiftisch'),
  T('Schmieden: +%s%% auf alle Attribute der Ausrüstung','Am Amboss bei ungeschmiedeter Ausrüstung verwenden','Am Amboss bei Ausrüstung +%s verwenden','Schmiedestufe +%s: +%s%% auf alle Attribute','Diamantbonus: +%s%% auf alle Werte','Angriffstempo +30%%','Baut die ganze Erzader ab (bis zu 10 Blöcke)','Fällt den ganzen Baum (bis zu 64 Stämme)','Baut 3x3 Blöcke auf einmal ab','Erntet und bepflanzt bis zu 10 Pflanzen per Rechtsklick neu','1/%s Chance auf 1 zusätzlichen Gegenstand','+%s%% EP (Blöcke und Mobs)','Bewegungsgeschwindigkeit +%s%%','Ausweichen: %s%% Chance, Schaden zu ignorieren','Magnetisch: zieht Gegenstände im Umkreis von %s Blöcken an','Federleicht: -%s%% Fallschaden','%s-Set (%s/4)'),
  S('Schleifscheibe dreht sich','Schleiftisch bereit','Edelstein geschliffen','Schleifer schabt am Stein'),
  H('Nimm Feuerstein aus der Leiste','Lege den Feuerstein auf die Spitze des Schleifers','Nimm ein Roherz aus der Leiste','Lege das Erz in die Mitte der Scheibe','Knopf drücken zum Schleifen  (Umschalt: alles schleifen)','Schleife...','Das gehört da nicht hin','Esc: verlassen  ·  Rechtsklick: Gegenstand zurücklegen','Mehr Gegenstände aus dem Inventar','Im Inventar gibt es nichts mehr zu schleifen'),
  D('Rubin: ein roter Kristall mit intensivem Glanz.','Saphir: ein blauer Kristall, kalt beim Anfassen.','Jade: ein glatter grüner Stein, sehr widerstandsfähig.')),

 'it_it': L(G('Rubino','Zaffiro','Giada','Diamante'),
  ['Spada','Piccone','Pala','Ascia','Zappa','Elmo','Corazza','Gambali','Stivali'],
  P('{n} di {g}','Minerale di {g}','Minerale di {g} in ardesia profonda','Blocco di {g}'),
  R('Diamante grezzo','Rubino grezzo','Zaffiro grezzo','Giada grezza'),
  U('Pietra della forgia','Frammento di Pietra Filosofale','Pietra Filosofale incompleta','Pietra Filosofale piccola','Pietra Filosofale grande','Minerale della forgia','Blocco di Pietra Filosofale','Tavolo da lapidario'),
  T("Forgia: +%s%% a tutti gli attributi dell'equipaggiamento","Usala sull'incudine con un equipaggiamento non forgiato","Usala sull'incudine con un equipaggiamento +%s",'Forgia +%s: +%s%% a tutti gli attributi','Bonus diamante: +%s%% a tutte le statistiche',"Velocità d'attacco +30%%",'Rompe l\'intero filone di minerale (fino a 10 blocchi)',"Abbatte l'intero albero (fino a 64 tronchi)",'Rompe 3x3 blocchi in una volta','Raccoglie e ripianta fino a 10 colture con il tasto destro','1/%s di probabilità di rilasciare 1 oggetto extra','+%s%% di XP (blocchi e mob)','Velocità di movimento +%s%%','Schivata: %s%% di probabilità di ignorare un danno','Magnetico: attira oggetti entro %s blocchi','Leggero come una piuma: -%s%% di danno da caduta','Set di %s (%s/4)'),
  S('Il disco del lapidario gira','Tavolo da lapidario pronto','Gemma tagliata','Il lapidario raschia la pietra'),
  H('Prendi della selce dalla barra',"Metti la selce sulla punta del lapidario",'Prendi un minerale grezzo dalla barra','Metti il minerale al centro del disco','Premi il pulsante per tagliare  (Maiusc: taglia tutto)','Taglio in corso...','Questo oggetto non va lì',"Esc: esci  ·  Tasto destro: rimetti l'oggetto","Altri oggetti dell'inventario","Non c'è altro da tagliare nell'inventario"),
  D('Rubino: cristallo rosso dal bagliore intenso.','Zaffiro: cristallo blu, freddo al tatto.','Giada: pietra verde e liscia, molto resistente.'),
  of=dict(rubi='rubino',sapphire='zaffiro',jade='giada'), gn=dict(rubi='rubino',sapphire='zaffiro',jade='giada')),

 'pt_pt': L(G('Rubi','Safira','Jade','Diamante'),
  ['Espada','Picareta','Pá','Machado','Enxada','Capacete','Peitoral','Calças','Botas'],
  P('{n} de {g}','Minério de {g}','Minério de {g} de ardósia','Bloco de {g}'),
  R('Diamante Bruto','Rubi Bruto','Safira Bruta','Jade Bruto'),
  U('Pedra de Forja','Fragmento de Pedra Filosofal','Pedra Filosofal Incompleta','Pedra Filosofal Pequena','Pedra Filosofal Grande','Minério de Forja','Bloco de Pedra Filosofal','Mesa de Lapidação'),
  T('Forja: +%s%% em todos os atributos do equipamento','Use na bigorna num equipamento sem forja','Use na bigorna num equipamento +%s','Forja +%s: +%s%% em todos os atributos','Bónus de diamante: +%s%% em todas as estatísticas','Velocidade de ataque +30%%','Parte o filão inteiro de minério (até 10 blocos)','Derruba a árvore inteira (até 64 troncos)','Parte 3x3 blocos de uma vez','Colhe e replanta até 10 plantações com o botão direito','1/%s de probabilidade de largar 1 item extra','+%s%% de XP (blocos e mobs)','Velocidade de movimento +%s%%','Esquiva: %s%% de probabilidade de ignorar um dano','Magnético: atrai itens num raio de %s blocos','Peso-pluma: -%s%% de dano de queda','Conjunto de %s (%s/4)'),
  S('O disco de lapidação gira','Mesa de lapidação pronta','Gema lapidada','O lapidador raspa a pedra'),
  H('Apanhe um sílex da barra','Coloque o sílex na ponta do lapidador','Apanhe um minério bruto da barra','Coloque o minério no centro do disco','Prima o botão para lapidar  (Shift: lapidar tudo)','A lapidar...','Esse item não vai aí','Esc: sair  ·  Botão direito: devolver o item','Mais itens do inventário','Não há mais nada para lapidar no inventário'),
  D('Rubi: cristal vermelho de brilho intenso.','Safira: cristal azul, frio ao toque.','Jade: pedra verde e lisa, muito resistente.')),

 'nl_nl': L(G('Robijn','Saffier','Jade','Diamant'),
  ['zwaard','houweel','schep','bijl','schoffel','helm','borstplaat','beenbeschermers','laarzen'],
  P('{g} {n}','{g}-erts','Dieplei-{g}-erts','{g}blok'),
  R('Ruwe diamant','Ruwe robijn','Ruwe saffier','Ruwe jade'),
  U('Smeedsteen','Fragment van de Steen der Wijzen','Onvolledige Steen der Wijzen','Kleine Steen der Wijzen','Grote Steen der Wijzen','Smeederts','Blok van de Steen der Wijzen','Edelsteenslijptafel'),
  T('Smeden: +%s%% op alle eigenschappen van de uitrusting','Gebruik op het aambeeld met ongesmede uitrusting','Gebruik op het aambeeld met +%s uitrusting','Smeden +%s: +%s%% op alle eigenschappen','Diamantbonus: +%s%% op alle waarden','Aanvalssnelheid +30%%','Breekt de hele ertsader (tot 10 blokken)','Velt de hele boom (tot 64 stammen)','Breekt 3x3 blokken tegelijk','Oogst en plant tot 10 gewassen opnieuw met rechtsklik','1/%s kans op 1 extra voorwerp','+%s%% XP (blokken en mobs)','Bewegingssnelheid +%s%%','Ontwijken: %s%% kans om schade te negeren','Magnetisch: trekt voorwerpen aan binnen %s blokken','Veerlicht: -%s%% valschade','%s-set (%s/4)'),
  S('Slijpschijf draait','Slijptafel klaar','Edelsteen geslepen','Slijper schraapt de steen'),
  H('Pak vuursteen uit de balk','Leg de vuursteen op de punt van de slijper','Pak een ruw erts uit de balk','Leg het erts in het midden van de schijf','Druk op de knop om te slijpen  (Shift: alles slijpen)','Slijpen...','Dat voorwerp hoort daar niet','Esc: sluiten  ·  Rechtsklik: voorwerp terugleggen','Meer voorwerpen uit de inventaris','Er is niets meer te slijpen in de inventaris'),
  D('Robijn: een rode kristal met een intense glans.','Saffier: een blauwe kristal, koud om aan te raken.','Jade: een gladde groene steen, erg sterk.'),
  of=dict(rubi='Robijnen',sapphire='Saffieren',jade='Jaden'), gn=dict(rubi='Robijn',sapphire='Saffier',jade='Jade')),
}
COPY = {'es_mx':'es_es','es_ar':'es_es','es_cl':'es_es','es_ec':'es_es','es_uy':'es_es','es_ve':'es_es','fr_ca':'fr_fr','de_at':'de_de','de_ch':'de_de','nl_be':'nl_nl'}
